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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Value("${docker.command.timeout:60}")
    private int dockerTimeoutSeconds;

    /**
     * OCR 增强变体并行执行的线程池。
     * 设计要点：
     * 1) 使用守护线程，避免阻塞 JVM 退出。
     * 2) 并发度由 {@link #ocrParallelism} 控制（默认 6，刚好覆盖 6 个增强变体）。
     *    实际上限受宿主机 CPU 与 docker 守护进程能承载的并行子进程数限制，
     *    可通过配置项 {@code ocr.parallelism} 调整。
     */
    private volatile ExecutorService ocrExecutor;

    @Value("${ocr.parallelism:6}")
    private int ocrParallelism;

    /**
     * 增强识别阶段的总时间预算（秒）。
     * 所有变体并行执行，总耗时不超过此值；超时的变体会被取消。
     * 默认 120 秒，根据生产环境 Docker Tesseract 实际耗时调整。
     */
    @Value("${ocr.enhanced.total.timeout.seconds:120}")
    private int enhancedTotalTimeoutSeconds;

    private synchronized ExecutorService getOcrExecutor() {
        if (ocrExecutor == null) {
            int parallelism = Math.max(1, ocrParallelism);
            final AtomicInteger counter = new AtomicInteger(0);
            ocrExecutor = Executors.newFixedThreadPool(parallelism, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ocr-variant-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return ocrExecutor;
    }

    public String recognize(MultipartFile file) throws IOException, InterruptedException {
        File savedFile = saveToSharedDir(file);
        String ocrText = execTesseract(savedFile);
        savedFile.delete();
        return ocrText;
    }

    /**
     * 生成多个OCR候选结果：
     * 1. 原图直接识别
     * 2. 原始结果信号不足时，并行执行多个增强变体（规范化PNG / 上半屏灰度 / 灰度对比度增强 / 旋转90/180/270）
     *
     * 性能优化：增强变体不再串行执行，改为线程池并行执行，整体耗时由
     * "N × 单变体耗时" 缩短为约 "1 × 单变体耗时"（受 CPU/Docker 并发能力影响）。
     */
    public List<OcrResult> recognizeCandidates(MultipartFile file) throws IOException, InterruptedException {
        File savedFile = saveToSharedDir(file);
        List<File> tempFiles = new ArrayList<>();
        List<OcrResult> results = new ArrayList<>();
        try {
            long originalStart = System.currentTimeMillis();
            String originalText = execTesseract(savedFile);
            log.info("OCR变体[original]执行耗时: {} ms", System.currentTimeMillis() - originalStart);
            addOcrResult(results, "original", originalText);

            if (shouldTryEnhancedRecognition(originalText)) {
                BufferedImage sourceImage = readImageQuietly(savedFile);
                if (sourceImage != null) {
                    // ===== 1) 串行生成增强变体的图片文件（CPU 操作，耗时短） =====
                    List<VariantTask> variantTasks = new ArrayList<>();
                    try {
                        File normalizedFile = createNormalizedVariant(sourceImage);
                        tempFiles.add(normalizedFile);
                        variantTasks.add(new VariantTask("normalized_full", normalizedFile));

                        File upperGrayFile = createUpperGrayVariant(sourceImage);
                        tempFiles.add(upperGrayFile);
                        variantTasks.add(new VariantTask("upper_gray", upperGrayFile));

                        File upperGrayContrastFile = createUpperGrayContrastVariant(sourceImage);
                        tempFiles.add(upperGrayContrastFile);
                        variantTasks.add(new VariantTask("upper_gray_contrast", upperGrayContrastFile));

                        // 旋转变体：覆盖横屏/倒置拍摄场景（如手机横放拍摄设备标识符页面）
                        // Tesseract 默认无法识别旋转90°/180°/270°的文字，需要预先正向化
                        for (int degrees : new int[]{90, 180, 270}) {
                            File rotatedFile = createRotatedVariant(sourceImage, degrees);
                            tempFiles.add(rotatedFile);
                            variantTasks.add(new VariantTask("rotated_" + degrees, rotatedFile));
                        }
                    } catch (IOException e) {
                        log.warn("生成OCR增强变体图片失败，将仅使用已生成的部分变体", e);
                    }

                    // ===== 2) 并行调用 docker tesseract 识别每个增强变体 =====
                    long enhancedStart = System.currentTimeMillis();
                    ExecutorService executor = getOcrExecutor();
                    List<Future<VariantOcrResult>> futures = new ArrayList<>(variantTasks.size());
                    for (final VariantTask task : variantTasks) {
                        futures.add(executor.submit(new Callable<VariantOcrResult>() {
                            @Override
                            public VariantOcrResult call() {
                                long t0 = System.currentTimeMillis();
                                String text = null;
                                try {
                                    text = execTesseract(task.imageFile);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    log.warn("增强OCR候选识别被中断，variant={}", task.variantName);
                                } catch (Exception e) {
                                    log.warn("增强OCR候选识别失败，variant={}", task.variantName, e);
                                }
                                long cost = System.currentTimeMillis() - t0;
                                log.info("OCR变体[{}]执行耗时: {} ms", task.variantName, cost);
                                return new VariantOcrResult(task.variantName, text);
                            }
                        }));
                    }

                    // ===== 3) 回收结果：带超时控制和提前退出 =====
                    long enhancedDeadline = System.currentTimeMillis() + enhancedTotalTimeoutSeconds * 1000L;
                    for (Future<VariantOcrResult> future : futures) {
                        long remaining = enhancedDeadline - System.currentTimeMillis();
                        if (remaining <= 0) {
                            future.cancel(true);
                            log.warn("增强OCR总时间预算已耗尽（{}s），跳过剩余变体", enhancedTotalTimeoutSeconds);
                            continue;
                        }
                        try {
                            VariantOcrResult variantResult = future.get(remaining, TimeUnit.MILLISECONDS);
                            addOcrResult(results, variantResult.variantName, variantResult.text);
                            // 提前退出：已获得包含设备信号的有效结果时，取消剩余变体
                            if (variantResult.text != null && containsDeviceSignal(variantResult.text)) {
                                log.info("增强OCR已获得有效结果（变体={}），取消剩余变体", variantResult.variantName);
                                for (Future<VariantOcrResult> f : futures) {
                                    f.cancel(true);
                                }
                                break;
                            }
                        } catch (TimeoutException te) {
                            future.cancel(true);
                            log.warn("增强OCR变体执行超时（剩余预算={}ms），跳过", Math.max(0, remaining));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            // 中断时取消剩余任务后退出
                            for (Future<VariantOcrResult> f : futures) {
                                f.cancel(true);
                            }
                            throw ie;
                        } catch (ExecutionException ee) {
                            log.warn("获取增强OCR变体结果失败", ee.getCause());
                        }
                    }
                    log.info("OCR增强变体并行执行总耗时: {} ms（变体数={}）",
                            System.currentTimeMillis() - enhancedStart, variantTasks.size());
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

    /**
     * 描述一个待执行的 OCR 增强变体任务（变体名 + 已生成的图片文件）。
     */
    private static class VariantTask {
        final String variantName;
        final File imageFile;

        VariantTask(String variantName, File imageFile) {
            this.variantName = variantName;
            this.imageFile = imageFile;
        }
    }

    /**
     * 一个变体的 OCR 执行结果（文本可能为 null，表示该变体识别失败）。
     */
    private static class VariantOcrResult {
        final String variantName;
        final String text;

        VariantOcrResult(String variantName, String text) {
            this.variantName = variantName;
            this.text = text;
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
        // 没有任何设备信号关键字（"序列号"/"IMEI"/"型号"等）时，无论是否识别出"长字母数字混合串"，
        // 都强制触发增强识别。原因：旋转拍摄/倒置拍摄时 OCR 会产出大量乱码长串（如 SV278890601299Z3Y），
        // 这些乱码本身长度合法但语义错误，必须通过旋转变体重试才能拿到正确文本。
        boolean noDeviceSignal = signalLines == 0;
        // 修正：当已有长字母数字混合串（IMEI/SN等）且行数足够时，
        // 即使没有中文设备信号关键字，也不再触发增强识别。
        // 避免因 Tesseract 未识别出中文标签（如"序列号"）而浪费时间做旋转等变体。
        boolean shouldEnhance;
        if (hasLongMixedToken && usefulLines >= 2) {
            shouldEnhance = lowConfidence || suspiciousAboutPage;
            // lowConfidence 在 hasLongMixedToken 为 true 时必然为 false，
            // 所以此处实际只有 suspiciousAboutPage 可能触发
        } else {
            shouldEnhance = lowConfidence || suspiciousAboutPage || noDeviceSignal;
        }
        if (shouldEnhance) {
            log.info("OCR触发增强识别: usefulLines={}, signalLines={}, hasLongMixedToken={}, hasAboutPageSignal={}, hasModelLine={}, hasSnLine={}, hasSuspiciousChars={}, noDeviceSignal={}",
                    usefulLines, signalLines, hasLongMixedToken, hasAboutPageSignal, hasModelLine, hasSnLine, hasSuspiciousChars, noDeviceSignal);
        }
        return shouldEnhance;
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

    /**
     * 创建旋转变体：将原图按指定角度顺时针旋转后输出 PNG。
     * 用于覆盖手机横放/倒置拍摄场景（Tesseract 对旋转文字识别能力有限）。
     *
     * @param sourceImage 原始图像
     * @param degrees     旋转角度，支持 90 / 180 / 270
     */
    private File createRotatedVariant(BufferedImage sourceImage, int degrees) throws IOException {
        BufferedImage rgbImage = toRgbImage(sourceImage);
        BufferedImage rotated = rotateImage(rgbImage, degrees);
        return writeTempPng(rotated, "rotated-" + degrees);
    }

    /**
     * 顺时针旋转图像。仅支持 90/180/270 度，其他角度按 0 处理（直接返回原图）。
     */
    private BufferedImage rotateImage(BufferedImage sourceImage, int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        if (normalized == 0) {
            return sourceImage;
        }
        int srcWidth = sourceImage.getWidth();
        int srcHeight = sourceImage.getHeight();
        int dstWidth = (normalized == 180) ? srcWidth : srcHeight;
        int dstHeight = (normalized == 180) ? srcHeight : srcWidth;

        BufferedImage rotated = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotated.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, dstWidth, dstHeight);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform transform = new AffineTransform();
        // 先平移到目标画布中心，再旋转，再平移回去（保证旋转后图像完整落在画布内）
        transform.translate(dstWidth / 2.0, dstHeight / 2.0);
        transform.rotate(Math.toRadians(normalized));
        transform.translate(-srcWidth / 2.0, -srcHeight / 2.0);

        graphics.drawImage(sourceImage, transform, null);
        graphics.dispose();
        return rotated;
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

        // 在后台线程读取输出，避免管道缓冲区满导致进程卡死，同时不阻塞超时判断
        StringBuilder sb = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                // 进程被销毁时可能抛出异常，忽略
            }
        }, "ocr-output-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        boolean finished = process.waitFor(dockerTimeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            readerThread.interrupt();
            throw new RuntimeException("Docker OCR 执行超时（" + dockerTimeoutSeconds + "s）");
        }
        // 等待读取线程结束（正常结束时很快完成）
        try {
            readerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String output = sb.toString();
        if (process.exitValue() != 0) {
            throw new RuntimeException("Docker OCR 执行失败, 输出: " + output);
        }
        return output;
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