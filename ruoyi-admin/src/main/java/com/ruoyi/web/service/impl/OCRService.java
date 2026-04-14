package com.ruoyi.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OCRService {

    private static final String DOCKER_IMAGE = "tesseractshadow/tesseract4re:latest";
    private static final String CONTAINER_DATA_DIR = "/data";
    private static final double UPPER_CROP_LEFT_RATIO = 0.08D;
    private static final double UPPER_CROP_TOP_RATIO = 0.08D;
    private static final double UPPER_CROP_WIDTH_RATIO = 0.84D;
    private static final double UPPER_CROP_HEIGHT_RATIO = 0.62D;
    private static final double UPPER_CROP_SCALE_RATIO = 2.0D;

    /**
     * OCR共享目录：宿主机与Docker容器之间的文件交换目录
     * macOS/Linux 默认: /tmp/ocr_shared
     * Windows 需在配置文件中设置为: D:/ocr_shared
     */
    @Value("${ocr.shared.dir:/tmp/ocr_shared}")
    private String sharedDir;

    @Value("${docker.command.timeout:30}")
    private int dockerTimeoutSeconds;

    public String recognize(MultipartFile file) throws IOException, InterruptedException {
        File savedFile = saveToSharedDir(file);
        String ocrText = execTesseract(savedFile);
        savedFile.delete();
        return ocrText;
    }

    /**
     * 生成多个OCR候选结果：
     * 1. 原图直接识别
     * 2. 原始结果信号不足时，补充规范化PNG识别
     * 3. 原始结果信号不足时，补充上半屏裁切+放大+灰度增强识别
     */
    public List<OcrResult> recognizeCandidates(MultipartFile file) throws IOException, InterruptedException {
        File savedFile = saveToSharedDir(file);
        List<File> tempFiles = new ArrayList<>();
        List<OcrResult> results = new ArrayList<>();
        try {
            String originalText = execTesseract(savedFile);
            addOcrResult(results, "original", originalText);

            if (shouldTryEnhancedRecognition(originalText)) {
                BufferedImage sourceImage = readImageQuietly(savedFile);
                if (sourceImage != null) {
                    File normalizedFile = createNormalizedVariant(sourceImage);
                    tempFiles.add(normalizedFile);
                    addOcrResult(results, "normalized_full", tryExecTesseract(normalizedFile, "normalized_full"));

                    File upperGrayFile = createUpperGrayVariant(sourceImage);
                    tempFiles.add(upperGrayFile);
                    addOcrResult(results, "upper_gray", tryExecTesseract(upperGrayFile, "upper_gray"));

                    File upperGrayContrastFile = createUpperGrayContrastVariant(sourceImage);
                    tempFiles.add(upperGrayContrastFile);
                    addOcrResult(results, "upper_gray_contrast", tryExecTesseract(upperGrayContrastFile, "upper_gray_contrast"));
                } else {
                    log.warn("图片增强识别已跳过，无法读取图片文件: {}", savedFile.getName());
                }
            }

            if (results.isEmpty()) {
                results.add(new OcrResult("original", originalText == null ? "" : originalText));
            }
            return results;
        } finally {
            deleteQuietly(savedFile);
            for (File tempFile : tempFiles) {
                deleteQuietly(tempFile);
            }
        }
    }

    private boolean shouldTryEnhancedRecognition(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            log.info("OCR原始结果为空，触发增强识别");
            return true;
        }

        int usefulLines = 0;
        int signalLines = 0;
        boolean hasLongMixedToken = false;
        boolean hasAboutPageSignal = false;
        boolean hasModelLine = false;
        boolean hasSnLine = false;
        boolean hasSuspiciousChars = false;

        for (String rawLine : ocrText.split("\\r?\\n")) {
            String line = rawLine.replaceAll("[\\f\\r]", "").trim();
            if (line.isEmpty() || isNoiseLine(line)) {
                continue;
            }
            usefulLines++;
            if (containsDeviceSignal(line)) {
                signalLines++;
            }

            if (line.contains("关于本机")) {
                hasAboutPageSignal = true;
            }
            if (line.contains("型号")) {
                hasModelLine = true;
            }
            if (line.contains("序列号")) {
                hasSnLine = true;
            }
            if (line.contains("人") || line.contains("匀") || line.contains("RRA") || line.contains("Zz")) {
                hasSuspiciousChars = true;
            }

            String compact = line.toUpperCase().replaceAll("[^A-Z0-9]", "");
            boolean hasLetter = compact.matches(".*[A-Z].*");
            boolean hasDigit = compact.matches(".*\\d.*");
            if (compact.length() >= 8 && hasLetter && hasDigit) {
                hasLongMixedToken = true;
            }
        }

        boolean lowConfidence = !hasLongMixedToken && (usefulLines <= 3 || signalLines == 0);
        boolean suspiciousAboutPage = hasAboutPageSignal && hasModelLine && hasSnLine && hasSuspiciousChars;
        if (lowConfidence || suspiciousAboutPage) {
            log.info("OCR触发增强识别: usefulLines={}, signalLines={}, hasLongMixedToken={}, hasAboutPageSignal={}, hasModelLine={}, hasSnLine={}, hasSuspiciousChars={}",
                    usefulLines, signalLines, hasLongMixedToken, hasAboutPageSignal, hasModelLine, hasSnLine, hasSuspiciousChars);
        }
        return lowConfidence || suspiciousAboutPage;
    }

    private boolean isNoiseLine(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("warning:")
                || lower.startsWith("warning ")
                || lower.startsWith("estimating ")
                || lower.startsWith("detected ");
    }

    private boolean containsDeviceSignal(String line) {
        String lower = line.toLowerCase();
        return line.contains("序列号")
                || line.contains("型号")
                || line.contains("关于本机")
                || line.contains("名称")
                || lower.contains("serial")
                || lower.contains("imei")
                || lower.contains("meid")
                || lower.contains("iphone")
                || lower.contains("android");
    }

    private void addOcrResult(List<OcrResult> results, String variantName, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String normalized = text.trim();
        for (OcrResult result : results) {
            if (result.getText().trim().equals(normalized)) {
                return;
            }
        }
        results.add(new OcrResult(variantName, text));
    }

    private BufferedImage readImageQuietly(File imageFile) {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            log.warn("读取图片用于增强识别失败: {}", imageFile.getName(), e);
            return null;
        }
    }

    private String tryExecTesseract(File imageFile, String variantName) throws InterruptedException {
        try {
            return execTesseract(imageFile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            log.warn("增强OCR候选识别失败，variant={}", variantName, e);
            return null;
        }
    }

    private File createNormalizedVariant(BufferedImage sourceImage) throws IOException {
        return writeTempPng(toRgbImage(sourceImage), "normalized");
    }

    private File createUpperGrayVariant(BufferedImage sourceImage) throws IOException {
        BufferedImage rgbImage = toRgbImage(sourceImage);
        int cropX = (int) Math.round(rgbImage.getWidth() * UPPER_CROP_LEFT_RATIO);
        int cropY = (int) Math.round(rgbImage.getHeight() * UPPER_CROP_TOP_RATIO);
        int cropWidth = (int) Math.round(rgbImage.getWidth() * UPPER_CROP_WIDTH_RATIO);
        int cropHeight = (int) Math.round(rgbImage.getHeight() * UPPER_CROP_HEIGHT_RATIO);

        BufferedImage cropped = cropImage(rgbImage, cropX, cropY, cropWidth, cropHeight);
        BufferedImage scaled = scaleImage(cropped, UPPER_CROP_SCALE_RATIO);
        BufferedImage gray = toGrayImage(scaled);
        return writeTempPng(gray, "upper-gray");
    }

    private File createUpperGrayContrastVariant(BufferedImage sourceImage) throws IOException {
        BufferedImage rgbImage = toRgbImage(sourceImage);
        int cropX = (int) Math.round(rgbImage.getWidth() * UPPER_CROP_LEFT_RATIO);
        int cropY = (int) Math.round(rgbImage.getHeight() * UPPER_CROP_TOP_RATIO);
        int cropWidth = (int) Math.round(rgbImage.getWidth() * UPPER_CROP_WIDTH_RATIO);
        int cropHeight = (int) Math.round(rgbImage.getHeight() * UPPER_CROP_HEIGHT_RATIO);

        BufferedImage cropped = cropImage(rgbImage, cropX, cropY, cropWidth, cropHeight);
        BufferedImage scaled = scaleImage(cropped, UPPER_CROP_SCALE_RATIO);
        BufferedImage gray = toGrayImage(scaled);
        BufferedImage contrast = adjustContrast(gray, 1.6f, 8f);
        return writeTempPng(contrast, "upper-gray-contrast");
    }

    private File writeTempPng(BufferedImage image, String suffix) throws IOException {
        File tempFile = new File(sharedDir, UUID.randomUUID() + "-" + suffix + ".png");
        ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

    private BufferedImage toRgbImage(BufferedImage sourceImage) {
        if (sourceImage.getType() == BufferedImage.TYPE_INT_RGB) {
            return sourceImage;
        }
        BufferedImage rgbImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
        graphics.drawImage(sourceImage, 0, 0, null);
        graphics.dispose();
        return rgbImage;
    }

    private BufferedImage cropImage(BufferedImage sourceImage, int x, int y, int width, int height) {
        int safeX = Math.max(0, Math.min(x, sourceImage.getWidth() - 1));
        int safeY = Math.max(0, Math.min(y, sourceImage.getHeight() - 1));
        int safeWidth = Math.max(1, Math.min(width, sourceImage.getWidth() - safeX));
        int safeHeight = Math.max(1, Math.min(height, sourceImage.getHeight() - safeY));

        BufferedImage subImage = sourceImage.getSubimage(safeX, safeY, safeWidth, safeHeight);
        BufferedImage copy = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(subImage, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private BufferedImage scaleImage(BufferedImage sourceImage, double scaleRatio) {
        int scaledWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scaleRatio));
        int scaledHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scaleRatio));
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawRenderedImage(sourceImage, AffineTransform.getScaleInstance(scaleRatio, scaleRatio));
        graphics.dispose();
        return scaledImage;
    }

    private BufferedImage toGrayImage(BufferedImage sourceImage) {
        BufferedImage grayImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(sourceImage, grayImage);
        return grayImage;
    }

    private BufferedImage adjustContrast(BufferedImage sourceImage, float scaleFactor, float offset) {
        RescaleOp op = new RescaleOp(scaleFactor, offset, null);
        return op.filter(sourceImage, null);
    }

    private void deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!file.delete()) {
            log.debug("临时文件删除失败，将依赖系统后续清理: {}", file.getAbsolutePath());
        }
    }

    private File saveToSharedDir(MultipartFile file) throws IOException {
        File dir = new File(sharedDir);
        if (!dir.exists()) dir.mkdirs();

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        File target = new File(dir, UUID.randomUUID() + ext);
        Files.copy(file.getInputStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private String execTesseract(File imageFile) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(Arrays.asList(
                "docker", "run", "--rm", "-v",
                sharedDir + ":" + CONTAINER_DATA_DIR,
                DOCKER_IMAGE,
                "tesseract",
                CONTAINER_DATA_DIR + "/" + imageFile.getName(),
                "stdout",
                "-l", "chi_sim+eng",
                // 页面分割模式：6=假设为统一的文本块（手机截图通常是单列列表布局）
                // 注：模式3(全自动)对复杂布局更好，但模式6对手机设置页面的单列文本更稳定
                "-c", "tessedit_pageseg_mode=6",
                // 不设置白名单，允许识别中文关键字（如"序列号"、"型号"等）
                // 禁用系统词典和频率词典，避免自动修正IMEI/SN中的字符
                "-c", "load_system_dawg=false",
                "-c", "load_freq_dawg=false",
                // 保留行间空格，有助于区分标签和值（如"序列号 JKQTP4LJ09"）
                "-c", "preserve_interword_spaces=1",
                "-c", "textord_space_size_is_variable=1",
                // 引擎模式：使用LSTM+传统（OEM_DEFAULT），兼顾准确率和兼容性
                "-c", "tessedit_ocr_engine_mode=3"
        ));
        // 移除了原来的 char_blacklist，因为白名单优先级更高且更明确

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = readProcessOutput(process);
        boolean finished = process.waitFor(dockerTimeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Docker OCR 执行超时");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Docker OCR 执行失败, 输出: " + output);
        }
        return output;
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static class OcrResult {
        private final String variantName;
        private final String text;

        public OcrResult(String variantName, String text) {
            this.variantName = variantName;
            this.text = text;
        }

        public String getVariantName() {
            return variantName;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * 简单的 MultipartFile 实现，仅用于测试。
     * 实际项目中可使用 Spring 的 MockMultipartFile。
     */
    private static class SimpleMultipartFile implements MultipartFile {
        private final File file;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        public SimpleMultipartFile(File file) {
            this.file = file;
            this.name = file.getName();
            this.originalFilename = file.getName();
            this.contentType = guessContentType(file.getName());
        }

        private String guessContentType(String fileName) {
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return file.length() == 0; }

        @Override
        public long getSize() { return file.length(); }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    // ================= 测试用 main 方法 =================
    public static void main(String[] args) {
        // 1. 准备测试图片路径（请修改为实际存在的图片文件）
        String testImagePath = "/tmp/ocr_shared/imei.jpg";   // ⚠️ 替换成你的图片路径
        File imageFile = new File(testImagePath);
        if (!imageFile.exists()) {
            System.err.println("测试图片不存在: " + testImagePath);
            return;
        }

        // 2. 将 File 包装为 MultipartFile
        MultipartFile multipartFile = new SimpleMultipartFile(imageFile);

        // 3. 创建 OCRService 实例并执行识别
        OCRService ocrService = new OCRService();
        // main方法中需手动设置注入的值
        ocrService.sharedDir = "/tmp/ocr_shared";
        ocrService.dockerTimeoutSeconds = 30;

        try {
            System.out.println("开始 OCR 识别...");
            String result = ocrService.recognize(multipartFile);
            System.out.println("识别结果：\n" + result);
        } catch (Exception e) {
            System.err.println("OCR 识别失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}