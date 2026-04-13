package com.ruoyi.web.service.impl;

import com.ruoyi.web.controller.tool.ImageOCRDockerLastController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OCRService {

    private static final String DOCKER_IMAGE = "tesseractshadow/tesseract4re:latest";
    private static final String SHARED_DIR = "D:/ocr_shared";
    private static final String CONTAINER_DATA_DIR = "/data";

    @Value("${docker.command.timeout:30}")
    private int dockerTimeoutSeconds;

    public String recognize(MultipartFile file) throws IOException, InterruptedException {
        File savedFile = saveToSharedDir(file);
        String ocrText = execTesseract(savedFile);
        savedFile.delete();
        return ocrText;
    }

    private File saveToSharedDir(MultipartFile file) throws IOException {
        File dir = new File(SHARED_DIR);
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
                SHARED_DIR + ":" + CONTAINER_DATA_DIR,
                DOCKER_IMAGE,
                "tesseract",
                CONTAINER_DATA_DIR + "/" + imageFile.getName(),
                "stdout",
                "-l", "chi_sim+eng",
                "-c", "tessedit_pageseg_mode=6",
                // 白名单：只允许数字和大小写字母
                "-c", "tessedit_char_whitelist=0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
                // 禁用系统词典和频率词典，避免自动修正
                "-c", "load_system_dawg=false",
                "-c", "load_freq_dawg=false",
                // 保留有用的空格控制参数
                "-c", "preserve_interword_spaces=0",
                "-c", "textord_space_size_is_variable=1",
                // 引擎模式：使用LSTM+传统（OEM_DEFAULT）
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
        String testImagePath = "D:\\ocr_shared\\imei.jpg";   // ⚠️ 替换成你的图片路径
        File imageFile = new File(testImagePath);
        if (!imageFile.exists()) {
            System.err.println("测试图片不存在: " + testImagePath);
            return;
        }

        // 2. 将 File 包装为 MultipartFile
        MultipartFile multipartFile = new SimpleMultipartFile(imageFile);

        // 3. 创建 OCRService 实例并执行识别
        OCRService ocrService = new OCRService();
        // 如果使用了 @Value 注入 timeout，main 中需手动设置（或直接使用默认值）
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