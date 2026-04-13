package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceInfoExtractorService {

    // ---------- 通用正则 ----------
    private static final Pattern IMEI_LOOSE = Pattern.compile("\\b\\d{15,17}\\b");
    // 增强关键字匹配：支持 IMEI、IMEI1、IMEI2 后跟冒号/空格/直接数字
    private static final Pattern IMEI_KEYWORD = Pattern.compile(
            "(?i)(?:IMEI1?|IMEI2?)\\s*[:：]?\\s*(\\d{15,17})"
    );

    // SN 通用模式（无品牌时使用）
    private static final Pattern SN_GENERIC = Pattern.compile("\\b[A-Z0-9]{8,22}\\b");

    // 常见停用词（不应作为SN）
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "ANDROID", "IPHONE", "XIAOMI", "HUAWEI", "SERIAL", "NUMBER",
            "WARNING", "ESTIMATING", "DETECTED", "IMEI", "SN", "S/N"
    ));

    // ---------- 主入口 ----------
    public DeviceInfo extractDeviceInfo(String ocrText, String brandType) {
        if (ocrText == null || ocrText.isEmpty()) {
            return new DeviceInfo("", "", "");
        }
        log.info("原始OCR文本: {}", ocrText);

        // 1. 清洗：过滤明显OCR噪声行
        List<String> cleanLines = Arrays.stream(ocrText.split("\\r?\\n"))
                .filter(line -> {
                    String lower = line.toLowerCase();
                    return !lower.contains("warning") && !lower.contains("estimating") && !lower.contains("detected");
                })
                .collect(Collectors.toList());

        // 2. 提取IMEI（带Luhn校验）
        List<String> imeiList = extractImei(cleanLines);
        String imei1 = imeiList.size() > 0 ? imeiList.get(0) : "";
        String imei2 = imeiList.size() > 1 ? imeiList.get(1) : "";

        // 3. 提取SN
        String sn = extractSn(cleanLines, brandType);

        log.info("最终提取结果: imei1={}, imei2={}, sn={}", imei1, imei2, sn);
        return new DeviceInfo(imei1, imei2, sn);
    }

    // ==================== IMEI 提取 ====================
    private List<String> extractImei(List<String> lines) {
        List<String> candidates = new ArrayList<>();

        // 第一遍：通过关键字精准定位（保留顺序）
        for (String line : lines) {
            Matcher m = IMEI_KEYWORD.matcher(line);
            while (m.find()) {
                String num = m.group(1);
                if (passLuhn(num)) candidates.add(num);
            }
        }

        // 第二遍：如果没有找到，则全文本扫描所有15~17位数字，并校验Luhn
        if (candidates.isEmpty()) {
            String fullText = String.join(" ", lines);
            Matcher m = IMEI_LOOSE.matcher(fullText);
            while (m.find()) {
                String num = m.group();
                if (passLuhn(num)) candidates.add(num);
            }
        }

        // 去重并限制最多2个（按原顺序）
        List<String> unique = candidates.stream().distinct().limit(2).collect(Collectors.toList());

        // 尝试区分IMEI1和IMEI2：如果文本中明确有IMEI2关键字，则调整顺序
        boolean hasImei2Keyword = lines.stream().anyMatch(l -> l.toLowerCase().contains("imei2"));
        if (hasImei2Keyword && unique.size() == 2) {
            // 假设第一个是IMEI1，第二个是IMEI2，无需调整
        }
        return unique;
    }

    // 标准 Luhn 算法（从右向左，偶数位乘2）
    private boolean passLuhn(String imei) {
        if (imei == null || imei.length() < 15) return false;
        int sum = 0;
        for (int i = imei.length() - 1; i >= 0; i--) {
            int digit = imei.charAt(i) - '0';
            if ((imei.length() - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit = digit / 10 + digit % 10;
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }

    // ==================== SN 提取 ====================
    private String extractSn(List<String> lines, String brandType) {
        // 策略1：通过关键字行提取
        String sn = extractSnByKeyword(lines, brandType);
        if (sn != null && !sn.isEmpty()) return sn;

        // 策略2：根据品牌特定模式提取（不依赖关键字）
        sn = extractSnByPattern(lines, brandType);
        if (sn != null && !sn.isEmpty()) return sn;

        // 策略3：通用模式扫描 + 评分排序
        sn = extractSnGeneric(lines);
        return sn != null ? sn : "";
    }

    private String extractSnByKeyword(List<String> lines, String brandType) {
        List<String> keywords = Arrays.asList("序列号", "sn", "s/n", "serial number", "serial no");
        Pattern brandPattern = getSnPattern(brandType);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase();
            boolean hasKeyword = keywords.stream().anyMatch(lower::contains);
            if (!hasKeyword) continue;

            // 从当前行提取候选
            String candidate = extractCandidateFromLine(line, brandPattern, true);
            if (candidate != null) return candidate;

            // 向后最多查3行
            for (int j = i + 1; j <= Math.min(i + 3, lines.size() - 1); j++) {
                candidate = extractCandidateFromLine(lines.get(j), brandPattern, true);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private String extractSnByPattern(List<String> lines, String brandType) {
        Pattern brandPattern = getSnPattern(brandType);
        for (String line : lines) {
            String candidate = extractCandidateFromLine(line, brandPattern, false);
            if (candidate != null) return candidate;
        }
        return null;
    }

    private String extractSnGeneric(List<String> lines) {
        List<SnCandidate> candidates = new ArrayList<>();
        for (String line : lines) {
            Matcher m = SN_GENERIC.matcher(line.toUpperCase());
            while (m.find()) {
                String token = m.group();
                if (isValidSnToken(token)) {
                    int score = scoreSnCandidate(token);
                    candidates.add(new SnCandidate(token, score));
                }
            }
        }
        if (candidates.isEmpty()) return null;
        // 按得分降序，取最高分
        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        return candidates.get(0).value;
    }

    private String extractCandidateFromLine(String line, Pattern pattern, boolean isKeywordLine) {
        String cleaned = line.replaceAll("\\s+", "").toUpperCase();
        Matcher m = pattern.matcher(cleaned);
        while (m.find()) {
            String candidate = m.group();
            if (!isValidSnToken(candidate)) continue;
            // 关键字行中若匹配到“IMEI”字样则跳过（避免误取）
            if (isKeywordLine && cleaned.contains("IMEI")) continue;
            return candidate;
        }
        return null;
    }

    private boolean isValidSnToken(String token) {
        if (token == null || token.length() < 6) return false;
        if (token.matches("\\d+")) return false;          // 纯数字（可能是IMEI）
        if (token.matches("[A-Z]+")) return false;       // 纯字母（可能是单词）
        String upper = token.toUpperCase();
        if (STOP_WORDS.contains(upper)) return false;
        // 排除包含明显IMEI数字串的（15-17位纯数字已在上面过滤）
        return true;
    }

    private int scoreSnCandidate(String token) {
        int score = 0;
        // 长度在10~18之间加分
        if (token.length() >= 10 && token.length() <= 18) score += 10;
        else if (token.length() > 18) score += 5;
        // 字母数字混合加分
        boolean hasLetter = token.matches(".*[A-Z].*");
        boolean hasDigit = token.matches(".*\\d.*");
        if (hasLetter && hasDigit) score += 20;
        // 不包含常见混淆词（如ANDROID）加分
        if (!token.toUpperCase().contains("ANDROID")) score += 5;
        return score;
    }

    // 根据品牌返回特定的SN正则（长度更精准）
    private Pattern getSnPattern(String brandType) {
        if ("apple".equalsIgnoreCase(brandType)) {
            // 苹果序列号：10~14位，通常不含小写
            return Pattern.compile("\\b[A-Z0-9]{10,14}\\b");
        } else if ("huawei".equalsIgnoreCase(brandType)) {
            // 华为SN：15~20位，首字母大写
            return Pattern.compile("\\b[A-Z][A-Z0-9]{14,19}\\b");
        } else if ("xiaomi".equalsIgnoreCase(brandType)) {
            // 小米SN：10~22位，全大写字母数字
            return Pattern.compile("\\b[A-Z0-9]{10,22}\\b");
        } else {
            return SN_GENERIC;
        }
    }

    // 辅助类
    private static class SnCandidate {
        String value;
        int score;
        SnCandidate(String v, int s) { value = v; score = s; }
    }

    // ==================== 测试入口（保留原测试数据） ====================
    public static void main(String[] args) {
        // 可分别测试 xiaomi / huawei / apple
        String type = "apple";
        String ocrText = getOcrText(type);
        DeviceInfoExtractorService service = new DeviceInfoExtractorService();
        System.out.println(service.extractDeviceInfo(ocrText, type));
    }


    private static String getOcrText(String type) {
        if (type.equals("xiaomi")) {
            return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                    "Estimating resolution as 498\n" +
                    "Detected 26 diacritics\n" +
                    "1005 KaeFFPsadolD\n" +
                    "SRSNSfae\n" +
                    "BE2VUIIJARVVTVL5VVUPI\n" +
                    "OShigas\n" +
                    "305020WOMCNXMC06\n" +
                    "AndroidhrZzs\n" +
                    "16AndroidZER20260201\n" +
                    "AES\n" +
                    "24122RKC7C\n" +
                    "MPSSDE70CN8605dbb173\n" +
                    "6677android158gca30f3b4bef6abogki4409747\n" +
                    "714k\n" +
                    "AEhRAR\n" +
                    "V1\n" +
                    "IMEI1\n" +
                    "862527071010804\n" +
                    "IMEI2\n" +
                    "862527071010812\n" +
                    "ULSFta\n" +
                    "OFAtefE2368GBBTFGE528GB4816GB";
        } else if (type.equals("huawei")) {
            return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                    "Estimating resolution as 394\n" +
                    "Detected 72 diacritics\n" +
                    "eee\n" +
                    "FL in\n" +
                    "IMEI353149596371672\n" +
                    "IMEI2353149593886086";
        } else if (type.equals("apple")) {
            return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                    "Estimating resolution as 448\n" +
                    "E\n" +
                    "oO\n" +
                    "zi\n" +
                    "my\n" +
                    "Had\n" +
                    "208AG\n" +
                    "AFA\n" +
                    "iPhonexmg\n" +
                    "146\n" +
                    "iPhone12\n" +
                    "MGGX3CHAA\n" +
                    "F17FV5CAODYP\n" +
                    "106\n" +
                    "1232\n" +
                    "51\n" +
                    "128GB\n" +
                    "2695GB\n" +
                    "945C9A3ED1FQ\f";
        }
        return "";
    }
}