package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 设备信息提取服务
 * 从OCR识别文本中提取手机的IMEI和SN码
 * 支持品牌：苹果、华为/荣耀、小米/红米
 *
 * 全链路识别策略：
 * 1. OCR文本预处理（噪声过滤、字符修正）
 * 2. IMEI提取（关键字定位 → 分行匹配 → 全文扫描，Luhn校验+容错）
 * 3. SN提取（关键字定位 → 品牌模式匹配 → 通用评分排序）
 */
@Slf4j
@Service
public class DeviceInfoExtractorService {

    // ==================== IMEI 相关常量与正则 ====================

    /**
     * IMEI关键字同行匹配（增强版）
     * 兼容格式：
     * - "IMEI 353149596371672"       华为共享设备标识符页面
     * - "IMEI2 353149593886086"      华为/鸭宝
     * - "IMEI1 862527071010804"      小米全部参数页面
     * - "IMEI 1 862527071010804"     小米（IMEI和序号之间有空格）
     * - "IMEI353149596371672"        无空格直接拼接
     * - "IMEI：353149596371672"      中文冒号
     * - "IMEI:353149596371672"       英文冒号
     * - "lMEI" / "1MEI"             OCR将I误识别为l或1
     * - "IMEI/MEID 353149596371672"  部分安卓手机显示格式
     */
    private static final Pattern IMEI_KEYWORD = Pattern.compile(
            "(?i)(?:[Il1]ME[Il1])(?:/MEID)?\\s*([12])?\\s*[:：]?\\s*(\\d{15,17})"
    );

    /**
     * IMEI标签独占一行的情况（小米手机常见）
     * 行1: "IMEI1" 或 "IMEI 1" 或 "IMEI"
     * 行2: "862527071010804"
     */
    private static final Pattern IMEI_LABEL_ONLY = Pattern.compile(
            "(?i)^\\s*(?:[Il1]ME[Il1])(?:/MEID)?\\s*([12])?\\s*[:：]?\\s*$"
    );

    /**
     * 中文IMEI关键字匹配
     * 部分手机截图中可能出现"国际移动设备识别码"等中文标签
     */
    private static final List<String> IMEI_CN_KEYWORDS = Arrays.asList(
            "国际移动设备识别码", "移动设备标识", "设备标识符"
    );

    /** 宽松匹配：全文扫描15~17位纯数字（前后不能紧邻其他数字） */
    private static final Pattern IMEI_LOOSE = Pattern.compile("(?<![\\d])(\\d{15,17})(?![\\d])");

    /**
     * IMEI数字修正映射：OCR常见的字符误识别
     * O→0, o→0, l→1, I→1, S→5, B→8, G→6, Z→2
     */
    private static final Map<Character, Character> IMEI_CHAR_FIX = new HashMap<>();
    static {
        IMEI_CHAR_FIX.put('O', '0');
        IMEI_CHAR_FIX.put('o', '0');
        IMEI_CHAR_FIX.put('l', '1');
        IMEI_CHAR_FIX.put('I', '1');
        IMEI_CHAR_FIX.put('S', '5');
        IMEI_CHAR_FIX.put('B', '8');
        IMEI_CHAR_FIX.put('G', '6');
        IMEI_CHAR_FIX.put('Z', '2');
    }

    // ==================== SN 相关常量与正则 ====================

    /** SN 通用模式：字母数字混合8~22位 */
    private static final Pattern SN_GENERIC = Pattern.compile("[A-Z0-9]{8,22}");

    /**
     * SN关键字列表（中英文），兼容各种手机截图和第三方查询工具：
     * - "序列号"（iPhone关于本机、鸭宝查询等）
     * - "serial number"、"serial no"
     * - "sn"、"s/n"
     * - "设备序列号"（部分安卓手机设置页面）
     */
    private static final List<String> SN_KEYWORDS = Arrays.asList(
            "序列号", "设备序列号", "sn", "s/n", "serial number", "serial no", "serialnumber"
    );

    /**
     * 停用词集合：不应作为SN的字符串
     * 包含品牌名、系统关键字、OCR噪声词等
     */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            // 品牌名
            "ANDROID", "IPHONE", "XIAOMI", "HUAWEI", "HONOR", "REDMI",
            // 系统/标签关键字
            "SERIAL", "NUMBER", "WARNING", "ESTIMATING", "DETECTED", "RESOLUTION", "DIACRITICS",
            "IMEI", "IMEI1", "IMEI2", "MEID",
            // 常见OCR噪声
            "INVALID", "INSTEAD", "USING", "STDOUT",
            // 存储容量
            "128GB", "256GB", "512GB", "1TB", "64GB", "32GB", "16GB", "8GB", "4GB",
            // 版本号
            "ANDROID16", "ANDROID15", "ANDROID14", "ANDROID13", "ANDROID12", "ANDROID11"
    ));

    /**
     * 停用模式列表（正则匹配），用于过滤更复杂的噪声字符串
     * 每个模式都有明确的过滤目的
     */
    private static final List<Pattern> STOP_PATTERNS = Arrays.asList(
            // 容量标识：128GB、1TB等
            Pattern.compile("^\\d+[GT]B$"),
            // 版本号：V1、V2等
            Pattern.compile("^V\\d+$"),
            // 入网型号：A3297等4位数字前缀
            Pattern.compile("^[A-Z]\\d{4}$"),
            // 纯数字（可能是IMEI或其他数字ID）
            Pattern.compile("^\\d+$"),
            // 短纯字母（<=6位，可能是单词/缩写/OCR噪声，如AES、AFA、ULSFta）
            Pattern.compile("^[A-Z]{1,6}$"),
            // 短数字+字母组合（如208AG、714K）
            Pattern.compile("^\\d{1,4}[A-Z]{1,3}$"),
            // 短字母+数字组合（如AES、V1）
            Pattern.compile("^[A-Z]{1,3}\\d{1,3}$"),
            // 包含Android关键字的噪声串
            Pattern.compile(".*ANDROID.*", Pattern.CASE_INSENSITIVE),
            // 包含iPhone关键字的噪声串
            Pattern.compile(".*IPHONE.*", Pattern.CASE_INSENSITIVE),
            // 基带版本号（如MPSSDE70CN8605dbb173）
            Pattern.compile(".*MPSS.*", Pattern.CASE_INSENSITIVE),
            // OS版本号（如305020WOMCNXMC06）
            Pattern.compile(".*WOMCNXM.*", Pattern.CASE_INSENSITIVE),
            // 内核版本（如gca30f3b4bef6a）
            Pattern.compile(".*GCA30F.*", Pattern.CASE_INSENSITIVE),
            // 型号号码格式（如MGGX3CHAA，苹果型号号码OCR后的结果）
            Pattern.compile("^[A-Z]{2,4}\\d[A-Z0-9]{2,6}$"),
            // 纯噪声短串（如eee、oO、zi、my、Had等）
            Pattern.compile("^.{1,3}$")
    );

    // ==================== 主入口 ====================

    /**
     * 从OCR文本中提取设备信息（IMEI1、IMEI2、SN）
     * 这是全链路的核心方法，按以下步骤执行：
     * 1. 文本预处理（清洗OCR噪声）
     * 2. IMEI提取（三级策略 + Luhn校验容错）
     * 3. SN提取（三级策略 + 品牌特征评分）
     *
     * @param ocrText   OCR识别的原始文本
     * @param brandType 品牌类型（apple_warranty / huawei / xiaomi）
     * @return DeviceInfo 包含IMEI1、IMEI2、SN
     */
    public DeviceInfo extractDeviceInfo(String ocrText, String brandType) {
        if (ocrText == null || ocrText.isEmpty()) {
            return new DeviceInfo("", "", "");
        }
        log.info("原始OCR文本:\n{}", ocrText);

        // 1. 文本预处理：清洗OCR噪声行
        List<String> cleanLines = preprocessOcrText(ocrText);
        log.info("清洗后文本行数: {}, 内容: {}", cleanLines.size(), cleanLines);

        // 2. 提取IMEI（带Luhn校验 + 容错机制）
        List<String> imeiList = extractImei(cleanLines);
        String imei1 = imeiList.size() > 0 ? imeiList.get(0) : "";
        String imei2 = imeiList.size() > 1 ? imeiList.get(1) : "";

        // 3. 提取SN（多策略级联，传入已识别的IMEI用于排除）
        String sn = extractSn(cleanLines, brandType, imei1, imei2);

        log.info("最终提取结果: imei1={}, imei2={}, sn={}", imei1, imei2, sn);
        return new DeviceInfo(imei1, imei2, sn);
    }

    // ==================== 文本预处理 ====================

    /**
     * OCR文本预处理：
     * 1. 按行分割并trim
     * 2. 去除Tesseract引擎警告行（Warning/Estimating/Detected）
     * 3. 去除空行
     * 4. 去除换页符等控制字符
     */
    private List<String> preprocessOcrText(String ocrText) {
        return Arrays.stream(ocrText.split("\\r?\\n"))
                .map(line -> line.replaceAll("[\\f\\r]", "").trim()) // 去除换页符等控制字符
                .filter(line -> !line.isEmpty())
                .filter(line -> {
                    String lower = line.toLowerCase();
                    // 过滤Tesseract引擎输出的警告信息
                    if (lower.startsWith("warning:") || lower.startsWith("warning ")) return false;
                    if (lower.startsWith("estimating ")) return false;
                    if (lower.startsWith("detected ")) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ==================== IMEI 提取 ====================

    /**
     * IMEI提取主逻辑，按优先级依次尝试三级策略：
     *
     * 策略1：关键字同行匹配（最精准）
     *   - 匹配 "IMEI 353149596371672" 等格式
     *   - 兼容OCR字符误识别（lMEI、1MEI等）
     *
     * 策略2：关键字+下一行匹配（处理分行情况）
     *   - 匹配 "IMEI1\n862527071010804" 等格式
     *   - 向下最多查找3行，支持中间有空行
     *   - 对下一行做字符修正（O→0等）
     *
     * 策略3：全文宽松扫描（兜底）
     *   - 扫描所有15~17位数字串
     *   - 仅在前两个策略都未找到时使用
     *
     * Luhn校验策略：
     *   - 通过校验 → 强候选
     *   - 未通过校验但有关键字上下文 → 弱候选（OCR可能识别错了个别数字）
     *   - 优先使用强候选，不足时用弱候选补充
     */
    private List<String> extractImei(List<String> lines) {
        List<ImeiCandidate> strongCandidates = new ArrayList<>();
        List<ImeiCandidate> weakCandidates = new ArrayList<>();

        // === 策略1：关键字同行匹配 ===
        for (String line : lines) {
            Matcher m = IMEI_KEYWORD.matcher(line);
            while (m.find()) {
                String indexStr = m.group(1); // "1" 或 "2" 或 null
                String num = m.group(2);
                num = normalizeImei(num);
                if (num == null) continue;

                int idx = parseImeiIndex(indexStr, line);
                addImeiCandidate(num, idx, strongCandidates, weakCandidates);
            }

            // 检查中文IMEI关键字（如"国际移动设备识别码"）
            for (String cnKw : IMEI_CN_KEYWORDS) {
                if (line.contains(cnKw)) {
                    // 从该行中提取数字串
                    Matcher numMatcher = Pattern.compile("\\d{15,17}").matcher(line);
                    while (numMatcher.find()) {
                        String num = normalizeImei(numMatcher.group());
                        if (num != null) {
                            addImeiCandidate(num, 1, strongCandidates, weakCandidates);
                        }
                    }
                }
            }
        }

        // === 策略2：关键字在单独行，数字在下一行 ===
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher labelMatcher = IMEI_LABEL_ONLY.matcher(line);
            if (!labelMatcher.matches()) continue;

            String indexStr = labelMatcher.group(1);
            int idx = parseImeiIndex(indexStr, line);

            // 向后查找最近的数字行（最多3行）
            for (int j = i + 1; j <= Math.min(i + 3, lines.size() - 1); j++) {
                String nextLine = lines.get(j).trim();
                if (nextLine.isEmpty()) continue;

                // 先尝试直接匹配纯数字
                if (nextLine.matches("\\d{15,17}")) {
                    String num = normalizeImei(nextLine);
                    if (num != null) {
                        addImeiCandidate(num, idx, strongCandidates, weakCandidates);
                    }
                    break;
                }

                // 尝试字符修正后匹配（处理OCR将0识别为O等情况）
                String fixed = fixImeiChars(nextLine);
                if (fixed.matches("\\d{15,17}")) {
                    String num = normalizeImei(fixed);
                    if (num != null) {
                        addImeiCandidate(num, idx, strongCandidates, weakCandidates);
                    }
                    break;
                }

                // 尝试去除空格/横杠后匹配（处理IMEI被分段的情况，如"8625 2707 1010 804"）
                String noSpace = nextLine.replaceAll("[\\s\\-]", "");
                if (noSpace.matches("\\d{15,17}")) {
                    String num = normalizeImei(noSpace);
                    if (num != null) {
                        addImeiCandidate(num, idx, strongCandidates, weakCandidates);
                    }
                    break;
                }

                // 如果下一行是另一个IMEI标签，停止查找
                if (IMEI_LABEL_ONLY.matcher(nextLine).matches()) break;
                // 如果下一行包含IMEI关键字+数字，停止（策略1会处理）
                if (IMEI_KEYWORD.matcher(nextLine).find()) break;
            }
        }

        // === 策略3：全文宽松扫描（仅在前两个策略都没找到时使用） ===
        if (strongCandidates.isEmpty() && weakCandidates.isEmpty()) {
            String fullText = String.join(" ", lines);
            Matcher m = IMEI_LOOSE.matcher(fullText);
            while (m.find()) {
                String num = normalizeImei(m.group(1));
                if (num != null) {
                    addImeiCandidate(num, 0, strongCandidates, weakCandidates);
                }
            }
        }

        // === 合并结果 ===
        return mergeImeiCandidates(strongCandidates, weakCandidates);
    }

    /**
     * 添加IMEI候选项，根据Luhn校验结果分为强/弱候选
     */
    private void addImeiCandidate(String num, int idx,
                                   List<ImeiCandidate> strong, List<ImeiCandidate> weak) {
        if (passLuhn(num)) {
            strong.add(new ImeiCandidate(num, idx, true));
        } else {
            weak.add(new ImeiCandidate(num, idx, false));
        }
    }

    /**
     * 合并IMEI候选项：优先强候选，不足时用弱候选补充，去重并排序
     */
    private List<String> mergeImeiCandidates(List<ImeiCandidate> strong, List<ImeiCandidate> weak) {
        List<ImeiCandidate> all = new ArrayList<>(strong);
        if (all.size() < 2) {
            all.addAll(weak);
        }

        // 去重（保持首次出现的顺序）
        List<ImeiCandidate> unique = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ImeiCandidate c : all) {
            if (seen.add(c.value)) {
                unique.add(c);
            }
        }

        // 按IMEI索引排序（IMEI1在前，IMEI2在后）
        unique.sort(Comparator.comparingInt(a -> a.index));

        return unique.stream().limit(2).map(c -> c.value).collect(Collectors.toList());
    }

    /**
     * 标准化IMEI：去除非数字字符，截取前15位
     * 有些OCR会多识别出校验位后的额外数字（16~17位），统一截取前15位
     */
    private String normalizeImei(String imei) {
        if (imei == null) return null;
        imei = imei.replaceAll("[^0-9]", "");
        if (imei.length() < 15) return null;
        return imei.substring(0, 15);
    }

    /**
     * IMEI字符修正：将OCR常见的误识别字符修正为正确的数字
     * 例如：O→0, l→1, I→1, S→5, B→8
     * 仅用于IMEI上下文中的数字串修正
     */
    private String fixImeiChars(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (IMEI_CHAR_FIX.containsKey(c)) {
                sb.append(IMEI_CHAR_FIX.get(c));
            }
            // 其他字符直接跳过
        }
        return sb.toString();
    }

    /**
     * 解析IMEI索引（1或2），用于排序
     * 优先使用正则捕获的数字索引，其次通过关键字内容判断
     */
    private int parseImeiIndex(String indexStr, String line) {
        if (indexStr != null) {
            return Integer.parseInt(indexStr);
        }
        String lower = line.toLowerCase();
        if (lower.contains("imei2") || lower.contains("imei 2")) return 2;
        return 1;
    }

    /**
     * 标准 Luhn 算法校验（ISO/IEC 7812）
     * IMEI号码的最后一位是校验位，通过Luhn算法计算
     * 只取前15位进行校验
     */
    private boolean passLuhn(String imei) {
        if (imei == null || imei.length() < 15) return false;
        String check = imei.substring(0, 15);
        int sum = 0;
        for (int i = check.length() - 1; i >= 0; i--) {
            int digit = check.charAt(i) - '0';
            if (digit < 0 || digit > 9) return false; // 非数字字符
            if ((check.length() - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit = digit / 10 + digit % 10;
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }

    /** IMEI候选项内部类 */
    private static class ImeiCandidate {
        final String value;
        final int index; // 1=IMEI1, 2=IMEI2, 0=未知
        final boolean luhnPassed;

        ImeiCandidate(String v, int idx, boolean luhn) {
            value = v;
            index = idx;
            luhnPassed = luhn;
        }
    }

    // ==================== SN 提取 ====================

    /**
     * SN提取主逻辑，按优先级依次尝试三级策略：
     *
     * 策略1：通过中英文关键字定位（最高优先级）
     *   - 在包含"序列号"/"SN"/"Serial Number"等关键字的行中提取
     *   - 支持关键字和值在同一行或相邻行
     *
     * 策略2：根据品牌特定模式匹配
     *   - 苹果：10~12位字母数字混合，通常以F/C/D/G/H/J/M/Q开头
     *   - 华为：16~20位，首字母大写
     *   - 小米：12~20位字母数字混合
     *
     * 策略3：通用模式扫描 + 智能评分排序（兜底）
     *   - 扫描所有8~22位字母数字混合串
     *   - 通过多维度评分选出最可能的SN
     *
     * @param lines     清洗后的文本行
     * @param brandType 品牌类型
     * @param imei1     已提取的IMEI1（用于排除）
     * @param imei2     已提取的IMEI2（用于排除）
     */
    private String extractSn(List<String> lines, String brandType, String imei1, String imei2) {
        Set<String> imeiSet = new HashSet<>();
        if (!imei1.isEmpty()) imeiSet.add(imei1);
        if (!imei2.isEmpty()) imeiSet.add(imei2);

        // 策略1：通过关键字行提取
        String sn = extractSnByKeyword(lines, brandType, imeiSet);
        if (sn != null && !sn.isEmpty()) {
            log.info("SN通过策略1（关键字）提取: {}", sn);
            return sn;
        }

        // 策略2：根据品牌特定模式提取
        sn = extractSnByBrandPattern(lines, brandType, imeiSet);
        if (sn != null && !sn.isEmpty()) {
            log.info("SN通过策略2（品牌模式）提取: {}", sn);
            return sn;
        }

        // 策略3：通用模式扫描 + 评分排序
        sn = extractSnGeneric(lines, brandType, imeiSet);
        if (sn != null && !sn.isEmpty()) {
            log.info("SN通过策略3（通用评分）提取: {}", sn);
            return sn;
        }

        return "";
    }

    /**
     * 策略1：通过关键字行提取SN
     * 支持以下场景：
     * - "序列号 JKQTP4LJ09"（鸭宝查询，同行）
     * - "序列号：F17FV5GA0DYP"（iPhone关于本机，同行带冒号）
     * - "序列号\nF17FV5GA0DYP"（关键字和值分行）
     * - "SN: UDU0219C14000257"（华为，英文关键字）
     * - "序列号 F17FV5GA\n0DYP"（OCR将SN断成两行，需要拼接）
     */
    private String extractSnByKeyword(List<String> lines, String brandType, Set<String> imeiSet) {
        Pattern brandPattern = getSnPattern(brandType);
        int minExpectedLen = getSnMinExpectedLength(brandType);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // 将行内容去空格后转小写，用于关键字匹配
            String lowerNoSpace = line.toLowerCase().replaceAll("\\s+", "");

            boolean hasKeyword = SN_KEYWORDS.stream()
                    .anyMatch(kw -> lowerNoSpace.contains(kw.toLowerCase().replaceAll("\\s+", "")));
            if (!hasKeyword) continue;

            // 尝试从当前行提取（关键字和值在同一行）
            String candidate = extractSnFromKeywordLine(line, brandPattern, imeiSet);
            if (candidate != null) {
                // 如果提取到的SN偏短，尝试拼接后续行内容来补全
                if (candidate.length() < minExpectedLen) {
                    String merged = tryMergeSnWithNextLines(candidate, lines, i + 1, brandPattern, imeiSet, minExpectedLen);
                    if (merged != null) {
                        log.info("SN拼接补全: {} -> {}", candidate, merged);
                        return merged;
                    }
                }
                return candidate;
            }

            // 向后最多查3行（关键字和值分行的情况）
            for (int j = i + 1; j <= Math.min(i + 3, lines.size() - 1); j++) {
                String nextLine = lines.get(j).trim();
                if (nextLine.isEmpty()) continue;
                // 如果下一行又是一个关键字行（如"版本类型"），停止查找
                if (isLabelLine(nextLine)) break;
                candidate = extractSnValueFromLine(nextLine, brandPattern, imeiSet);
                if (candidate != null) {
                    // 同样检查是否偏短，尝试拼接
                    if (candidate.length() < minExpectedLen) {
                        String merged = tryMergeSnWithNextLines(candidate, lines, j + 1, brandPattern, imeiSet, minExpectedLen);
                        if (merged != null) {
                            log.info("SN拼接补全: {} -> {}", candidate, merged);
                            return merged;
                        }
                    }
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * 尝试将偏短的SN与后续行内容拼接，补全被OCR断行的序列号
     * 场景：OCR将"F17FV5GA0DYP"识别成两行"F17FV5GA"和"0DYP"（或"ODYP"）
     *
     * @param currentSn     当前提取到的偏短SN
     * @param lines         所有文本行
     * @param startIdx      开始查找的行索引
     * @param brandPattern  品牌SN正则
     * @param imeiSet       已识别的IMEI集合
     * @param minExpectedLen 期望的最小SN长度
     * @return 拼接后的完整SN，如果拼接失败返回null
     */
    private String tryMergeSnWithNextLines(String currentSn, List<String> lines, int startIdx,
                                            Pattern brandPattern, Set<String> imeiSet, int minExpectedLen) {
        StringBuilder merged = new StringBuilder(currentSn);
        for (int k = startIdx; k <= Math.min(startIdx + 2, lines.size() - 1); k++) {
            String nextLine = lines.get(k).trim();
            if (nextLine.isEmpty()) continue;
            // 如果遇到标签行或IMEI行，停止拼接
            if (isLabelLine(nextLine) || isImeiLine(nextLine)) break;

            // 提取下一行中的字母数字部分，尝试拼接
            String nextUpper = nextLine.toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (nextUpper.isEmpty()) continue;

            merged.append(nextUpper);
            String mergedStr = merged.toString();

            // 检查拼接后是否匹配品牌模式且合法
            Matcher m = brandPattern.matcher(mergedStr);
            if (m.matches() && isValidSn(mergedStr, imeiSet) && mergedStr.length() >= minExpectedLen) {
                return mergedStr;
            }
        }
        return null;
    }

    /**
     * 获取品牌SN的期望最小长度
     * 用于判断提取到的SN是否偏短，需要尝试拼接补全
     */
    private int getSnMinExpectedLength(String brandType) {
        if (brandType == null) return 10;
        switch (brandType.toLowerCase()) {
            case "apple":
            case "apple_warranty":
                return 10; // 苹果序列号通常10~12位
            case "huawei":
                return 14; // 华为SN通常16~20位
            case "xiaomi":
                return 12; // 小米SN通常12~20位
            default:
                return 10;
        }
    }

    /**
     * 从包含关键字的行中提取SN值
     * 先定位关键字位置，截取关键字后面的部分作为值区域
     */
    private String extractSnFromKeywordLine(String line, Pattern brandPattern, Set<String> imeiSet) {
        String valuePart = line;
        // 找到关键字并截取其后面的内容
        for (String kw : SN_KEYWORDS) {
            int idx = valuePart.toLowerCase().indexOf(kw.toLowerCase());
            if (idx >= 0) {
                valuePart = valuePart.substring(idx + kw.length());
                break;
            }
        }
        // 去除前导的冒号、空格等分隔符
        valuePart = valuePart.replaceAll("^[\\s:：/]+", "").trim();

        if (valuePart.isEmpty()) return null;

        // 在值部分中匹配品牌模式
        String upper = valuePart.toUpperCase().replaceAll("\\s+", "");
        Matcher m = brandPattern.matcher(upper);
        while (m.find()) {
            String candidate = m.group();
            if (isValidSn(candidate, imeiSet)) return candidate;
        }
        return null;
    }

    /**
     * 从普通行中提取SN值（用于关键字下一行的情况）
     */
    private String extractSnValueFromLine(String line, Pattern brandPattern, Set<String> imeiSet) {
        String upper = line.toUpperCase().replaceAll("\\s+", "");
        Matcher m = brandPattern.matcher(upper);
        while (m.find()) {
            String candidate = m.group();
            if (isValidSn(candidate, imeiSet)) return candidate;
        }
        return null;
    }

    /**
     * 策略2：根据品牌特定模式提取SN（不依赖关键字）
     * 适用于OCR未能识别出"序列号"等关键字的情况
     * 使用品牌特定的正则模式 + 评分排序
     * 注意：跳过包含IMEI关键字的行，避免从IMEI行中误提取SN
     */
    private String extractSnByBrandPattern(List<String> lines, String brandType, Set<String> imeiSet) {
        Pattern brandPattern = getSnPattern(brandType);
        List<SnCandidate> candidates = new ArrayList<>();

        for (String line : lines) {
            // 跳过包含IMEI关键字的行，避免从"IMEI353149596371672"中误提取SN
            if (isImeiLine(line)) continue;

            String upper = line.toUpperCase().replaceAll("\\s+", "");
            Matcher m = brandPattern.matcher(upper);
            while (m.find()) {
                String candidate = m.group();
                if (isValidSn(candidate, imeiSet)) {
                    int score = scoreSnCandidate(candidate, brandType);
                    candidates.add(new SnCandidate(candidate, score));
                }
            }
        }

        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        return candidates.get(0).value;
    }

    /**
     * 策略3：通用模式扫描 + 评分排序（兜底策略）
     * 扫描所有符合通用SN格式的字符串，通过多维度评分选出最可能的SN
     * 注意：跳过包含IMEI关键字的行
     */
    private String extractSnGeneric(List<String> lines, String brandType, Set<String> imeiSet) {
        List<SnCandidate> candidates = new ArrayList<>();
        for (String line : lines) {
            // 跳过包含IMEI关键字的行
            if (isImeiLine(line)) continue;

            String upper = line.toUpperCase();
            Matcher m = SN_GENERIC.matcher(upper);
            while (m.find()) {
                String token = m.group();
                if (isValidSn(token, imeiSet)) {
                    int score = scoreSnCandidate(token, brandType);
                    candidates.add(new SnCandidate(token, score));
                }
            }
        }
        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        return candidates.get(0).value;
    }

    // ==================== SN 验证与评分 ====================

    /**
     * 验证候选SN是否合法
     * 多重过滤条件：
     * 1. 长度检查（>=6位）
     * 2. 排除纯数字（可能是IMEI或其他数字ID）
     * 3. 排除包含"IMEI"关键字的候选（如OCR将"IMEI353149596371672"部分截取）
     * 4. 排除已识别的IMEI及其数字子串
     * 5. 排除停用词和停用模式
     */
    private boolean isValidSn(String token, Set<String> imeiSet) {
        if (token == null || token.length() < 6) return false;

        String upper = token.toUpperCase();

        // 排除纯数字
        if (upper.matches("\\d+")) return false;

        // 排除包含"IMEI"关键字的候选
        // 场景：OCR识别出"IMEI353149596371672"，品牌模式截取到"IMEI3531495963"，这不是SN
        if (upper.contains("IMEI") || upper.contains("MEID")) return false;

        // 排除已识别的IMEI（完全匹配或包含关系）
        for (String imei : imeiSet) {
            if (upper.equals(imei) || upper.contains(imei) || imei.contains(upper)) return false;
        }

        // 提取候选中的纯数字部分，检查是否与IMEI有重叠
        // 场景：候选"IMEI3531495963"去掉字母后是"3531495963"，是IMEI的前缀
        String digitsOnly = upper.replaceAll("[^0-9]", "");
        if (digitsOnly.length() >= 6) {
            for (String imei : imeiSet) {
                // 候选的数字部分是IMEI的前缀或子串
                if (imei.contains(digitsOnly) || digitsOnly.contains(imei)) return false;
                // 候选的数字部分与IMEI前N位重合（至少6位）
                if (imei.startsWith(digitsOnly) || digitsOnly.startsWith(imei.substring(0, Math.min(imei.length(), digitsOnly.length())))) return false;
            }
        }

        // 排除停用词
        if (STOP_WORDS.contains(upper)) return false;

        // 排除匹配停用模式的
        for (Pattern p : STOP_PATTERNS) {
            if (p.matcher(upper).matches()) return false;
        }

        return true;
    }

    /**
     * SN候选评分算法
     * 多维度评分，分数越高越可能是真正的SN
     *
     * 评分维度：
     * 1. 字母数字混合（+30分，SN的核心特征）
     * 2. 品牌特定长度匹配（+25分）
     * 3. 品牌特定首字符匹配（+15分）
     * 4. 通用长度范围（+10分）
     * 5. 不含噪声子串（+5分）
     */
    private int scoreSnCandidate(String token, String brandType) {
        int score = 0;

        // 字母数字混合加分（SN的核心特征）
        boolean hasLetter = token.matches(".*[A-Z].*");
        boolean hasDigit = token.matches(".*\\d.*");
        if (hasLetter && hasDigit) score += 30;

        // 根据品牌特定特征加分
        if ("apple_warranty".equalsIgnoreCase(brandType) || "apple".equalsIgnoreCase(brandType)) {
        // 苹果序列号：通常10~12位，8~9位的偏短候选降低评分
            if (token.length() >= 10 && token.length() <= 12) score += 25;
            else if (token.length() >= 8 && token.length() <= 9) score += 5;
            else if (token.length() > 12 && token.length() <= 14) score += 15;
            // 苹果序列号常见首字符：F/C/D/G/H/J/M/Q
            if (token.matches("^[FCDGHJMQ].*")) score += 15;
            // 苹果序列号不会太长
            if (token.length() > 14) score -= 10;
        } else if ("huawei".equalsIgnoreCase(brandType)) {
            // 华为SN：通常16~20位，首字母大写
            if (token.length() >= 16 && token.length() <= 20) score += 25;
            else if (token.length() >= 14 && token.length() <= 22) score += 15;
        } else if ("xiaomi".equalsIgnoreCase(brandType)) {
            // 小米SN：通常12~20位
            if (token.length() >= 12 && token.length() <= 20) score += 25;
            else if (token.length() >= 10 && token.length() <= 22) score += 15;
        }

        // 通用长度范围加分
        if (token.length() >= 10 && token.length() <= 18) score += 10;

        // 不包含常见噪声子串加分
        if (!token.contains("ANDROID") && !token.contains("IPHONE") && !token.contains("MPSS")
                && !token.contains("WOMCNXM") && !token.contains("GCA30F")) {
            score += 5;
        }

        return score;
    }

    /**
     * 根据品牌返回特定的SN正则模式
     * 针对苹果、华为、小米三个主流品牌优化
     */
    private Pattern getSnPattern(String brandType) {
        if (brandType == null) return SN_GENERIC;

        switch (brandType.toLowerCase()) {
            case "apple":
            case "apple_warranty":
                // 苹果序列号：8~14位，大写字母+数字混合
                // 例如：F17FV5GA0DYP（12位）、JKQTP4LJ09（10位）
                // 注意：最小长度保持8位以兼容关键字行提取（偏短时会触发拼接补全逻辑）
                return Pattern.compile("[A-Z0-9]{8,14}");

            case "huawei":
                // 华为/荣耀SN：通常16~20位，首字母大写
                // 例如：UDU0219C14000257（16位）
                return Pattern.compile("[A-Z][A-Z0-9]{13,19}");

            case "xiaomi":
                // 小米/红米SN：通常12~22位，全大写字母数字
                // 例如：0HFKJD8F2L1M（12位）
                return Pattern.compile("[A-Z0-9]{10,22}");

            default:
                return SN_GENERIC;
        }
    }

    /**
     * 判断一行是否为IMEI相关行
     * 用于在SN提取策略2/3中跳过IMEI行，避免从IMEI行中误提取SN
     * 例如："IMEI353149596371672"、"IMEI 353149596371672"、"IMEI2 353149593886086"
     */
    private boolean isImeiLine(String line) {
        if (line == null) return false;
        String lower = line.toLowerCase().replaceAll("\\s+", "");
        // 包含imei关键字的行
        if (lower.contains("imei") || lower.contains("meid")) return true;
        // 包含中文IMEI关键字的行
        for (String cnKw : IMEI_CN_KEYWORDS) {
            if (line.contains(cnKw)) return true;
        }
        return false;
    }

    /**
     * 判断一行是否为标签行（包含中文标签关键字）
     * 用于在向下查找SN值时，遇到新标签行时停止
     */
    private boolean isLabelLine(String line) {
        // 常见的手机信息标签
        List<String> labels = Arrays.asList(
                "版本类型", "入网型号", "是否正品", "激活状态", "激活日期", "保修状态",
                "型号", "处理器", "内存", "存储", "电池", "系统版本",
                "IMEI", "MEID", "蓝牙地址", "WiFi地址", "MAC地址"
        );
        for (String label : labels) {
            if (line.contains(label)) return true;
        }
        return false;
    }

    /** SN候选辅助类 */
    private static class SnCandidate {
        final String value;
        final int score;
        SnCandidate(String v, int s) { value = v; score = s; }
    }

    // ==================== 测试入口 ====================

    public static void main(String[] args) {
        DeviceInfoExtractorService service = new DeviceInfoExtractorService();

        System.out.println("========== 测试1：华为共享设备标识符页面（IMEI+空格+数字） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("huawei_share"), "huawei"));

        System.out.println("\n========== 测试2：iPhone关于本机页面（SN无关键字，靠品牌模式识别） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("apple"), "apple_warranty"));

        System.out.println("\n========== 测试3：小米全部参数页面（IMEI标签和数字分行） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("xiaomi"), "xiaomi"));

        System.out.println("\n========== 测试4：鸭宝查询结果页面（序列号关键字+IMEI） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("apple_yabao"), "apple_warranty"));

        System.out.println("\n========== 测试5：华为IMEI无空格拼接 ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("huawei"), "huawei"));

        System.out.println("\n========== 测试6：IMEI中间有空格/横杠 ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("imei_with_space"), "huawei"));

        System.out.println("\n========== 测试7：OCR字符误识别（O→0, l→1） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("ocr_char_error"), "xiaomi"));

        System.out.println("\n========== 测试8：华为荣耀手机（SN+IMEI完整页面） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("honor"), "huawei"));

        System.out.println("\n========== 测试9：红米手机 ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("redmi"), "xiaomi"));

        System.out.println("\n========== 测试10：苹果只有IMEI没有SN（不应误提取SN） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("apple_imei_only"), "apple_warranty"));

        System.out.println("\n========== 测试11：苹果序列号被OCR断行（F17FV5GA + 0DYP） ==========");
        System.out.println(service.extractDeviceInfo(getOcrText("apple_sn_split"), "apple_warranty"));
    }

    /**
     * 测试用OCR文本数据，模拟各种手机截图的OCR识别结果
     * 覆盖场景：
     * 1. 华为共享设备标识符页面
     * 2. iPhone关于本机页面
     * 3. 小米全部参数与信息页面
     * 4. 鸭宝查询结果页面
     * 5. 华为IMEI无空格拼接
     * 6. IMEI中间有空格/横杠
     * 7. OCR字符误识别
     * 8. 华为荣耀手机完整页面
     * 9. 红米手机
     */
    private static String getOcrText(String type) {
        switch (type) {
            case "xiaomi":
                // 小米"全部参数与信息"页面 —— IMEI标签和数字分行
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

            case "huawei":
                // 华为IMEI无空格拼接
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 394\n" +
                        "Detected 72 diacritics\n" +
                        "eee\n" +
                        "FL in\n" +
                        "IMEI353149596371672\n" +
                        "IMEI2353149593886086";

            case "huawei_share":
                // 华为"共享设备标识符"页面 —— IMEI+空格+数字
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 394\n" +
                        "IMEI 353149596371672\n" +
                        "IMEI2 353149593886086";

            case "apple":
                // iPhone"关于本机"页面 —— 无IMEI，SN需要靠品牌模式识别
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

            case "apple_yabao":
                // 鸭宝查询结果页面 —— 有"序列号"关键字
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 500\n" +
                        "IPHONE 16 PRO MAX BLACK 256GB DEMOCHN\n" +
                        "IMEI 353149596371672\n" +
                        "IMEI2 353149593886086\n" +
                        "序列号 JKQTP4LJ09\n" +
                        "版本类型 国行\n" +
                        "入网型号 A3297\n" +
                        "是否正品机 是\n" +
                        "激活状态 已激活\n" +
                        "激活日期 20241014\n" +
                        "保修状态 已过保修期";

            case "imei_with_space":
                // IMEI中间有空格/横杠的情况
                return "IMEI1\n" +
                        "8625 2707 1010 804\n" +
                        "IMEI2\n" +
                        "862527071010812";

            case "ocr_char_error":
                // OCR字符误识别：O→0, l→1
                return "IMEI1\n" +
                        "86252707l0l0804\n" +
                        "IMEI2\n" +
                        "862527071010812";

            case "honor":
                // 华为荣耀手机完整页面
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 450\n" +
                        "荣耀Magic6 Pro\n" +
                        "型号 BVL-AN00\n" +
                        "序列号 UDU0219C14000257\n" +
                        "IMEI 860012345678901\n" +
                        "IMEI2 860012345678919\n" +
                        "系统版本 MagicOS 8.0\n" +
                        "Android 14";

            case "redmi":
                // 红米手机
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 480\n" +
                        "Redmi Note 13 Pro\n" +
                        "IMEI 1 867432109876543\n" +
                        "IMEI 2 867432109876550\n" +
                        "Android 14\n" +
                        "MIUI 15";

            case "apple_imei_only":
                // 苹果只有IMEI没有SN的截图（本次bug场景）
                // OCR识别出"IMEI353149596371672"，不应从中误提取SN
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 394\n" +
                        "Detected 72 diacritics\n" +
                        "IMEI353149596371672\n" +
                        "IMEI2353149593886086";

            case "apple_sn_split":
                // 苹果序列号被OCR断成两行的场景
                // 实际序列号：F17FV5GA0DYP（12位），OCR在GA和0DYP之间断行
                return "Warning: Invalid resolution 0 dpi. Using 70 instead.\n" +
                        "Estimating resolution as 448\n" +
                        "iPhonexmg\n" +
                        "146\n" +
                        "iPhone12\n" +
                        "MGGX3CHAA\n" +
                        "序列号 F17FV5GA\n" +
                        "0DYP\n" +
                        "106\n" +
                        "1232\n" +
                        "128GB";

            default:
                return "";
        }
    }
}