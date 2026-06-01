package com.ruoyi.web.controller.tool;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.model.DeviceInfo;
import com.ruoyi.web.service.impl.DeviceInfoExtractorService;
import com.ruoyi.web.service.impl.OCRService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api("OCR图片识别")
@Slf4j
@RestController
@RequestMapping("/image/ocr")
public class ImageOCRController extends BaseController {

    @Autowired
    private OCRService ocrService;

    @Autowired
    private DeviceInfoExtractorService extractorService;

    @ApiOperation("识别IMEI/SN")
    @PostMapping("/imchect")
    public R<DeviceInfo> deviceInfo(@RequestParam("typeCode") String typeCode, @RequestParam("file") MultipartFile file) {
        String type = PhoneType.getValueByCode(typeCode);
        if (type == null) {
            return R.fail("查询失败，无效的手机类型！");
        };
        try {
            List<OCRService.OcrResult> ocrCandidates = ocrService.recognizeCandidates(file);
            DeviceInfo bestInfo = null;
            int bestScore = Integer.MIN_VALUE;
            // 仅让"强变体"（score≥STRONG_SCORE_THRESHOLD，即SN+IMEI同时识别成功）参与SN投票
            // 这样能屏蔽乱码变体（score=140，仅识别到伪SN无IMEI）通过凑票影响最终结果
            Map<String, Integer> snVoteCount = new HashMap<>();
            // 跨变体合并 IMEI 用：收集所有强变体的 (info, ocrText)
            List<StrongVariant> strongVariants = new ArrayList<>();

            for (OCRService.OcrResult candidate : ocrCandidates) {
                DeviceInfo info = extractorService.extractDeviceInfo(candidate.getText(), type);
                int score = scoreDeviceInfo(info);
                log.info("OCR候选提取结果: variant={}, score={}, info={}", candidate.getVariantName(), score, info);

                String normalizedSn = normalize(info.getSN());
                // 仅强变体的SN进入投票池，IMEI同样仅来自强变体参与跨变体合并
                if (score >= STRONG_SCORE_THRESHOLD) {
                    if (!normalizedSn.isEmpty()) {
                        snVoteCount.put(normalizedSn, snVoteCount.getOrDefault(normalizedSn, 0) + 1);
                    }
                    strongVariants.add(new StrongVariant(info, candidate.getText()));
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestInfo = info;
                }
            }

            if (bestInfo == null) {
                return R.ok(new DeviceInfo("", "", ""));
            }

            // 跨变体合并 IMEI：从所有强变体中收集 IMEI，并通过 OCR 上下文判断 IMEI1/IMEI2 归属
            String mergedImei1 = bestInfo.getIMEI();
            String mergedImei2 = bestInfo.getIMEI2();
            if (!strongVariants.isEmpty()) {
                String[] merged = mergeImeiAcrossVariants(strongVariants);
                mergedImei1 = merged[0];
                mergedImei2 = merged[1];
                log.info("跨变体IMEI合并结果: imei1={}, imei2={}", mergedImei1, mergedImei2);
            }

            // 投票仅在已有≥1张强变体票时才覆盖 bestInfo.SN（防止纯乱码变体污染结果）
            String votedSn = chooseVotedSn(snVoteCount);
            String finalSn = (!votedSn.isEmpty()) ? votedSn : normalize(bestInfo.getSN());

            return R.ok(new DeviceInfo(
                    mergedImei1 == null ? "" : mergedImei1,
                    mergedImei2 == null ? "" : mergedImei2,
                    finalSn
            ));
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            return R.fail("识别失败: " + e.getMessage());
        }
    }

    /**
     * 强变体阈值：当 score >= 此值时，认为变体识别质量较高（SN+至少一个IMEI都成功）。
     * 计算依据：SN(100) + 长度(20) + 字母数字混合(20) + IMEI1(40) + 强信号加成(200) = 380；
     *          SN(100) + 长度(20) + 字母数字混合(20) + IMEI2(30) + 强信号加成(200) = 370；
     *          仅有SN无IMEI的弱变体最高分 = 140。
     * 阈值 200 能稳定区分两类。
     */
    private static final int STRONG_SCORE_THRESHOLD = 200;

    /**
     * IMEI 标签识别正则（用于跨变体合并时判断某个 IMEI 数字串属于 IMEI1 还是 IMEI2）
     * 兼容：IMEI1: / IMEI2: / IMEI 1 / IME11: (索引数字1被OCR与字母I连写) / IMEI： 等
     */
    private static final Pattern IMEI_INDEX_PATTERN = Pattern.compile(
            "(?i)[Il1]ME[Il1](?:/MEID)?\\s*([12])?\\s*[:：]?\\s*((?:\\d[\\s\\-]?){14,18}\\d)"
    );

    /**
     * 跨变体合并 IMEI：
     * 步骤1：收集所有强变体中提取到的 IMEI（去重，保留首次出现顺序）
     * 步骤2：对每个 IMEI，扫描所有强变体的原始 OCR 文本，统计它出现在 IMEI1 标签 vs IMEI2 标签 的次数
     * 步骤3：根据多数投票决定每个 IMEI 是 IMEI1 还是 IMEI2，输出 [imei1, imei2]
     *
     * 处理冲突：
     * - 若两个 IMEI 都更倾向 IMEI1，则按文本中先出现的填 IMEI1，后出现的填 IMEI2
     * - 若只识别到一个 IMEI 但被标为 IMEI2，则 IMEI1 留空
     */
    private String[] mergeImeiAcrossVariants(List<StrongVariant> variants) {
        // 步骤1：收集所有 IMEI（去重保序）
        LinkedHashMap<String, int[]> imeiVotes = new LinkedHashMap<>(); // value: [imei1票数, imei2票数]
        for (StrongVariant v : variants) {
            String i1 = normalize(v.info.getIMEI());
            String i2 = normalize(v.info.getIMEI2());
            if (!i1.isEmpty()) imeiVotes.computeIfAbsent(i1, k -> new int[2]);
            if (!i2.isEmpty()) imeiVotes.computeIfAbsent(i2, k -> new int[2]);
        }

        if (imeiVotes.isEmpty()) {
            return new String[]{"", ""};
        }

        // 步骤2：对每个 IMEI 通过扫描所有 OCR 文本投票判断索引归属
        for (StrongVariant v : variants) {
            Matcher m = IMEI_INDEX_PATTERN.matcher(v.ocrText);
            while (m.find()) {
                String indexStr = m.group(1);
                String numRaw = m.group(2);
                String num = numRaw == null ? "" : numRaw.replaceAll("[^0-9]", "");
                if (num.length() < 15) continue;
                num = num.substring(0, 15);
                if (!imeiVotes.containsKey(num)) continue;

                int idx = parseIndexFromMatch(indexStr, m.group());
                if (idx == 1) imeiVotes.get(num)[0]++;
                else if (idx == 2) imeiVotes.get(num)[1]++;
            }
        }

        // 步骤3：选出 IMEI1 和 IMEI2
        String imei1 = "";
        String imei2 = "";
        // 收集每个 IMEI 的（票差 = imei1票 - imei2票），>0 倾向 IMEI1，<0 倾向 IMEI2
        // 优先放置票差最大的（最确定的）到 imei1
        List<Map.Entry<String, int[]>> entries = new ArrayList<>(imeiVotes.entrySet());

        // 第一轮：找最强的 IMEI1 候选（票差最大）
        Map.Entry<String, int[]> bestImei1 = null;
        int bestImei1Score = Integer.MIN_VALUE;
        for (Map.Entry<String, int[]> e : entries) {
            int diff = e.getValue()[0] - e.getValue()[1];
            if (diff > bestImei1Score) {
                bestImei1Score = diff;
                bestImei1 = e;
            }
        }
        if (bestImei1 != null) imei1 = bestImei1.getKey();

        // 第二轮：从剩余里找 IMEI2（票差最小，即更倾向 IMEI2）
        Map.Entry<String, int[]> bestImei2 = null;
        int bestImei2Score = Integer.MAX_VALUE;
        for (Map.Entry<String, int[]> e : entries) {
            if (e == bestImei1) continue;
            int diff = e.getValue()[0] - e.getValue()[1];
            if (diff < bestImei2Score) {
                bestImei2Score = diff;
                bestImei2 = e;
            }
        }
        if (bestImei2 != null) imei2 = bestImei2.getKey();

        // 边界：如果只识别到一个 IMEI，且它的 IMEI2 票数 > IMEI1 票数，说明它本身是 IMEI2，imei1 应该留空
        if (entries.size() == 1) {
            int[] votes = entries.get(0).getValue();
            if (votes[1] > votes[0]) {
                return new String[]{"", entries.get(0).getKey()};
            }
            return new String[]{entries.get(0).getKey(), ""};
        }

        return new String[]{imei1, imei2};
    }

    /** 从正则匹配中解析 IMEI 索引（1 或 2），优先取捕获组数字，否则看关键字内容 */
    private int parseIndexFromMatch(String indexStr, String fullMatch) {
        if (indexStr != null && !indexStr.isEmpty()) {
            try {
                return Integer.parseInt(indexStr);
            } catch (NumberFormatException ignored) {}
        }
        String lower = fullMatch == null ? "" : fullMatch.toLowerCase();
        if (lower.contains("imei2") || lower.contains("imei 2")) return 2;
        return 1;
    }

    /** 强变体记录（含 OCR 原始文本，用于跨变体合并时判定 IMEI 归属） */
    private static class StrongVariant {
        final DeviceInfo info;
        final String ocrText;
        StrongVariant(DeviceInfo info, String ocrText) {
            this.info = info;
            this.ocrText = ocrText == null ? "" : ocrText;
        }
    }

    private int scoreDeviceInfo(DeviceInfo info) {
        if (info == null) {
            return Integer.MIN_VALUE;
        }

        int score = 0;
        String sn = normalize(info.getSN());
        String imei1 = normalize(info.getIMEI());
        String imei2 = normalize(info.getIMEI2());

        boolean hasSn = !sn.isEmpty();
        boolean hasImei1 = !imei1.isEmpty();
        boolean hasImei2 = !imei2.isEmpty();

        if (hasSn) {
            score += 100;
            // 长度落在常见SN范围（覆盖苹果10~12位 / 小米12~20位 / 华为16~20位）
            if (sn.length() >= 10 && sn.length() <= 22) {
                score += 20;
            }
            if (sn.matches(".*[A-Z].*") && sn.matches(".*\\d.*")) {
                score += 20;
            }
        }
        if (hasImei1) {
            score += 40;
        }
        if (hasImei2) {
            score += 30;
        }

        // 强信号加成：当 SN 和至少一个 IMEI 同时识别出来时，
        // 说明该变体的图像方向/质量正确（OCR 文本上下文有效），给予额外加分。
        // 这能避免：旋转拍摄的图片在 original 变体中产出"看似 SN 实为乱码"的字符串(如 SV278890601299Z3Y)
        // 抢占评分高位，导致正确方向的旋转变体（含完整 SN+IMEI）反被压制。
        if (hasSn && (hasImei1 || hasImei2)) {
            score += 200;
        }
        return score;
    }

    private String chooseVotedSn(Map<String, Integer> snVoteCount) {
        String bestSn = "";
        int bestVotes = 0;
        for (Map.Entry<String, Integer> entry : snVoteCount.entrySet()) {
            String sn = entry.getKey();
            int votes = entry.getValue();
            if (votes > bestVotes || (votes == bestVotes && sn.length() > bestSn.length())) {
                bestSn = sn;
                bestVotes = votes;
            }
        }
        return bestSn;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}