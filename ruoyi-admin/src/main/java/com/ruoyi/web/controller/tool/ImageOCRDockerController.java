package com.ruoyi.web.controller.tool;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api("OCR图片识别")
@RestController
@RequestMapping("/image/docker/ocr")
public class ImageOCRDockerController extends BaseController {

    // Docker 容器配置
    private static final String DOCKER_IMAGE = "tesseractshadow/tesseract4re:latest";
    private static final String CONTAINER_NAME = "tesseract-ocr";
    private static final String SHARED_DIR = "/tmp/ocr_shared";      // 宿主机共享目录
    private static final String CONTAINER_DATA_DIR = "/data";        // 容器内挂载点

    @Autowired
    private ISysConfigService configService;

    @Value("${docker.command.timeout:30}")
    private int dockerTimeoutSeconds;

    /**
     * 应用启动时检查并启动 Docker 容器
     */
//    @PostConstruct
//    public void initContainer() {
//        // 确保共享目录存在
//        File sharedDir = new File(SHARED_DIR);
//        if (!sharedDir.exists()) {
//            sharedDir.mkdirs();
//        }
//
//        // 检查容器是否已存在且运行
//        if (!isContainerRunning()) {
//            startContainer();
//        }
//    }

    /**
     * 检查容器是否运行中
     */
    private boolean isContainerRunning() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-q", "-f", "name=" + CONTAINER_NAME);
            Process process = pb.start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();
            return exitCode == 0 && !output.trim().isEmpty();
        } catch (Exception e) {
            System.err.println("检查容器状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 启动容器（如果不存在则创建并启动）
     */
    private void startContainer() {
        // 先尝试删除可能存在的停止容器
        try {
            ProcessBuilder rmPb = new ProcessBuilder("docker", "rm", "-f", CONTAINER_NAME);
            rmPb.start().waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("-d");
        command.add("--name");
        command.add(CONTAINER_NAME);
        command.add("-v");
        command.add(SHARED_DIR + ":" + CONTAINER_DATA_DIR);
        command.add(DOCKER_IMAGE);
        command.add("tail");
        command.add("-f");
        command.add("/dev/null");

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 先读取输出（避免缓冲区满导致阻塞）
            String output = readProcessOutput(process);

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("启动 Docker 容器超时");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("启动 Docker 容器失败，退出码: " + exitCode + ", 输出: " + output);
            }
            System.out.println("Docker 容器 " + CONTAINER_NAME + " 启动成功");
        } catch (Exception e) {
            System.err.println("启动容器异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("无法启动 OCR 容器", e);
        }
    }

    /**
     * 执行 Docker exec 命令
     */
    private String execInContainer(List<String> args) throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add("docker");
        fullCommand.add("exec");
        fullCommand.add(CONTAINER_NAME);
        fullCommand.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        boolean finished = process.waitFor(dockerTimeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Docker exec 命令执行超时");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Docker exec 命令失败，退出码: " + exitCode + ", 输出: " + output);
        }
        return output;
    }

    /**
     * 读取进程输出
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    @ApiOperation("识别IMEI")
    @PostMapping("/imchect")
    public R<IMEIObject> imchect(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();
        System.out.println("开始处理IMEI识别请求...");
        try {
            // 将文件保存到共享目录
            File imageFile = saveToSharedDir(file);
            IMEIObject imeiObject = checkImei(imageFile);
            // 删除临时文件
            imageFile.delete();

            long endTime = System.currentTimeMillis();
            System.out.println("IMEI识别处理完成，总耗时: " + (endTime - startTime) + "ms");
            return R.ok(imeiObject);
        } catch (IOException e) {
            e.printStackTrace();
            long endTime = System.currentTimeMillis();
            System.err.println("文件处理失败，耗时: " + (endTime - startTime) + "ms，错误: " + e.getMessage());
            return R.fail("文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            long endTime = System.currentTimeMillis();
            System.err.println("识别失败，耗时: " + (endTime - startTime) + "ms，错误: " + e.getMessage());
            return R.fail("识别失败: " + e.getMessage());
        }
    }

    /**
     * 将 MultipartFile 保存到共享目录，返回 File 对象
     */
    private File saveToSharedDir(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        // 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String uniqueName = UUID.randomUUID().toString() + ext;
        File targetFile = new File(SHARED_DIR, uniqueName);
        Files.copy(file.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        long endTime = System.currentTimeMillis();
        System.out.println("文件保存耗时: " + (endTime - startTime) + "ms");
        return targetFile;
    }

    /**
     * 通过长期运行的 Docker 容器执行 OCR
     */
    private IMEIObject checkImei(File imageFile) {
        System.out.println("开始OCR识别处理...");
        long ocrStartTime = System.currentTimeMillis();

        // 容器内文件路径
        String containerFilePath = CONTAINER_DATA_DIR + "/" + imageFile.getName();

        // 构建 Tesseract 命令参数
        List<String> tesseractArgs = new ArrayList<>();
        tesseractArgs.add("tesseract");
        tesseractArgs.add(containerFilePath);
        tesseractArgs.add("stdout");
        tesseractArgs.add("-l");
        tesseractArgs.add("chi_sim+eng");
        tesseractArgs.add("-c");
        tesseractArgs.add("tessedit_pageseg_mode=6");
        tesseractArgs.add("-c");
        tesseractArgs.add("preserve_interword_spaces=0");
        tesseractArgs.add("-c");
        tesseractArgs.add("textord_space_size_is_variable=1");
        tesseractArgs.add("-c");
        tesseractArgs.add("tessedit_char_blacklist=@#$%^&*()_+=-[]{}|;':\",./<>?~`");
        tesseractArgs.add("-c");
        tesseractArgs.add("tessedit_ocr_engine_mode=1");

        String result;
        try {
            result = execInContainer(tesseractArgs);
        } catch (Exception e) {
            System.err.println("Docker exec 执行失败: " + e.getMessage());
            e.printStackTrace();
            // 尝试重启容器
            restartContainer();
            return null;
        }

        long ocrEnd = System.currentTimeMillis();
        System.out.println("OCR识别耗时: " + (ocrEnd - ocrStartTime) + "ms");

        // 输出识别结果
        System.out.println("OCR 识别结果：");
        System.out.println("----------------------------------------");
        System.out.println(result);
        System.out.println("----------------------------------------");

        // 提取 IMEI
        long extractStart = System.currentTimeMillis();
        IMEIObject imeiObject = extractIMEIInfo(result);
        long extractEnd = System.currentTimeMillis();
        System.out.println("IMEI信息提取耗时: " + (extractEnd - extractStart) + "ms");
        System.out.println("\n组装成对象：");
        System.out.println(imeiObject.toString());

        long ocrTotalTime = System.currentTimeMillis() - ocrStartTime;
        System.out.println("OCR识别处理总耗时: " + ocrTotalTime + "ms");

        return imeiObject;
    }

    /**
     * 重启容器（简单实现）
     */
    private void restartContainer() {
        try {
            ProcessBuilder stopPb = new ProcessBuilder("docker", "stop", CONTAINER_NAME);
            stopPb.start().waitFor(5, TimeUnit.SECONDS);
            ProcessBuilder startPb = new ProcessBuilder("docker", "start", CONTAINER_NAME);
            startPb.start().waitFor(5, TimeUnit.SECONDS);
            System.out.println("容器 " + CONTAINER_NAME + " 已重启");
        } catch (Exception e) {
            System.err.println("重启容器失败，尝试重新创建");
            startContainer();
        }
    }

    // ---------- 以下 IMEI 提取相关方法与原代码完全一致，未改动 ----------
    public static IMEIObject extractIMEIInfo(String ocrText) {
        String imei1 = extractByRegex(ocrText, "IMEI[^\\d]*(\\d{15})");
        String imei2 = extractByRegex(ocrText, "IMEI2[^\\d]*(\\d{15})");
        if (imei1 == null || imei2 == null) {
            String[] lines = ocrText.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("IMEI ") && !line.startsWith("IMEI2")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d{15}")) {
                            imei1 = part;
                            break;
                        }
                    }
                }
                if (line.startsWith("IMEI2 ")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d{15}")) {
                            imei2 = part;
                            break;
                        }
                    }
                }
            }
        }
        if (imei1 == null) {
            imei1 = extractIMEIByPattern(ocrText, "IMEI");
        }
        if (imei2 == null) {
            imei2 = extractIMEIByPattern(ocrText, "IMEI2");
        }
        return new IMEIObject(
                imei1 != null ? imei1 : "",
                imei2 != null ? imei2 : ""
        );
    }

    private static String extractByRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractIMEIByPattern(String text, String imeiType) {
        String regex = imeiType + "[\\s:：\\-]*([0-9]{15})";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        regex = imeiType + "\\s+([0-9]{15})";
        pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    static class IMEIObject {
        private String IMEI;
        private String IMEI2;

        public IMEIObject(String imei, String imei2) {
            this.IMEI = imei;
            this.IMEI2 = imei2;
        }

        public String getIMEI() { return IMEI; }
        public String getIMEI2() { return IMEI2; }

        public String toJSON() {
            return "{\n    \"IMEI\": \"" + (IMEI != null ? IMEI : "") + "\",\n    \"IMEI2\": \"" + (IMEI2 != null ? IMEI2 : "") + "\"\n}";
        }

        @Override
        public String toString() {
            return toJSON();
        }
    }

    public static void main(String[] args) {
        // 测试用例
        String imagePath = "D:\\vedio\\imei.jpg";
        ImageOCRDockerController im = new ImageOCRDockerController();
//        im.initContainer(); // 启动容器
        im.checkImei(new File(imagePath));
    }
}