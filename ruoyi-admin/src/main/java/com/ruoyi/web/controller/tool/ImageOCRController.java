package com.ruoyi.web.controller.tool;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api("OCR图片识别")
@RestController
@RequestMapping("/image/ocr")
public class ImageOCRController extends BaseController {

    @Autowired
    private ISysConfigService configService;

    @ApiOperation("识别IMEI")
    @PostMapping("/imchect")
    public R<IMEIObject> imchect(@RequestParam("file") MultipartFile file) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        System.out.println("开始处理IMEI识别请求...");
        try {
            // 将MultipartFile转换为临时File
            File tempFile = convertMultipartFileToFile(file);
            // 调用check方法处理临时文件
            IMEIObject imeiObject = checkImei(tempFile);
            // 删除临时文件
            tempFile.delete();

            // 计算总耗时
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println("IMEI识别处理完成，总耗时: " + totalTime + "ms");

            return R.ok(imeiObject);
        } catch (IOException e) {
            e.printStackTrace();
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.err.println("文件处理失败，耗时: " + totalTime + "ms，错误: " + e.getMessage());
            return R.fail("文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.err.println("识别失败，耗时: " + totalTime + "ms，错误: " + e.getMessage());
            return R.fail("识别失败: " + e.getMessage());
        }
    }

    /**
     * 将MultipartFile转换为File
     */
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        // 创建临时文件
        Path tempFilePath = Files.createTempFile("ocr_", "_" + file.getOriginalFilename());
        // 将MultipartFile内容复制到临时文件
        Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        long endTime = System.currentTimeMillis();
        System.out.println("文件转换耗时: " + (endTime - startTime) + "ms");
        return tempFilePath.toFile();
    }

    /**
     * 重载check方法，接收File参数
     */
    private IMEIObject checkImei(File imageFile) {
        System.out.println("开始OCR识别处理...");
        long ocrStartTime = System.currentTimeMillis();
        // 指定 Tesseract 的语言数据文件路径
        String os = System.getProperty("os.name").toLowerCase();
        String tessDataPath;
        if (os.contains("win")) {
            tessDataPath = configService.selectConfigByKey(Constants.TESSERACT_OCR_PATH_WIN);
        } else {
            tessDataPath = configService.selectConfigByKey(Constants.TESSERACT_OCR_PATH_LINUX);
        }

        try {
            // 创建 Tesseract 实例
            ITesseract tesseract = new Tesseract();

            // 设置语言数据文件路径
            tesseract.setDatapath(tessDataPath);

            // 设置识别语言和参数
            tesseract.setLanguage("chi_sim+eng");
            tesseract.setTessVariable("tessedit_pageseg_mode", "6");
            tesseract.setTessVariable("preserve_interword_spaces", "0");
            tesseract.setTessVariable("textord_space_size_is_variable", "1");
            tesseract.setTessVariable("tessedit_char_blacklist", "@#$%^&*()_+=-[]{}|;':\",./<>?~`");
            tesseract.setTessVariable("tessedit_ocr_engine_mode", "1");
            // 执行 OCR
            long ocrStart = System.currentTimeMillis();
            // 执行 OCR
            String result = tesseract.doOCR(imageFile);

            long ocrEnd = System.currentTimeMillis();
            System.out.println("OCR识别耗时: " + (ocrEnd - ocrStart) + "ms");

            // 输出识别结果
            System.out.println("OCR 识别结果：");
            System.out.println("----------------------------------------");
            System.out.println(result);
            System.out.println("----------------------------------------");

            // 提取IMEI信息
            long extractStart = System.currentTimeMillis();
            IMEIObject imeiObject = extractIMEIInfo(result);
            long extractEnd = System.currentTimeMillis();
            System.out.println("IMEI信息提取耗时: " + (extractEnd - extractStart) + "ms");
            // 输出JSON格式
            System.out.println("\n组装成对象：");
            System.out.println(imeiObject.toString());

            long ocrEndTime = System.currentTimeMillis();
            long ocrTotalTime = ocrEndTime - ocrStartTime;
            System.out.println("OCR识别处理总耗时: " + ocrTotalTime + "ms");

            return imeiObject;
        } catch (TesseractException e) {
            System.err.println("Tesseract 识别出错: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("发生其他错误: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保留原来的check方法，但改为调用新的check(File)方法
     */
    private IMEIObject checkImei(String imagePath) {
        return checkImei(new File(imagePath));
    }

    /**
     * 从OCR识别结果中提取IMEI信息
     */
    public static IMEIObject extractIMEIInfo(String ocrText) {
        // 方法1：直接使用正则表达式匹配
        String imei1 = extractByRegex(ocrText, "IMEI[^\\d]*(\\d{15})");
        String imei2 = extractByRegex(ocrText, "IMEI2[^\\d]*(\\d{15})");

        // 方法2：如果正则没匹配到，尝试按行处理
        if (imei1 == null || imei2 == null) {
            String[] lines = ocrText.split("\n");
            for (String line : lines) {
                line = line.trim();

                // 匹配IMEI（第一行）
                if (line.startsWith("IMEI ") && !line.startsWith("IMEI2")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d{15}")) {
                            imei1 = part;
                            break;
                        }
                    }
                }

                // 匹配IMEI2
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

        // 方法3：使用更灵活的正则表达式
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

    /**
     * 使用正则表达式提取匹配项
     */
    private static String extractByRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 使用更灵活的模式提取IMEI
     */
    private static String extractIMEIByPattern(String text, String imeiType) {
        // 构建更灵活的正则表达式
        String regex = imeiType + "[\\s:：\\-]*([0-9]{15})";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // 尝试另一种模式：IMEI和数字之间可能有更多空格
        regex = imeiType + "\\s+([0-9]{15})";
        pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * IMEI对象类
     */
    static class IMEIObject {
        private String IMEI;
        private String IMEI2;

        public IMEIObject(String imei, String imei2) {
            this.IMEI = imei;
            this.IMEI2 = imei2;
        }

        public String getIMEI() {
            return IMEI;
        }

        public String getIMEI2() {
            return IMEI2;
        }

        /**
         * 转换为JSON字符串
         */
        public String toJSON() {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("    \"IMEI\": \"").append(IMEI != null ? IMEI : "").append("\",\n");
            json.append("    \"IMEI2\": \"").append(IMEI2 != null ? IMEI2 : "").append("\"\n");
            json.append("}");
            return json.toString();
        }

        @Override
        public String toString() {
            return toJSON();
        }
    }

    public static void main(String[] args) {
        // 测试原有的文件路径方式
        String imagePath = "D:\\vedio\\imei.jpg";
        ImageOCRController im  = new ImageOCRController();
        im.checkImei(imagePath);

    }
}