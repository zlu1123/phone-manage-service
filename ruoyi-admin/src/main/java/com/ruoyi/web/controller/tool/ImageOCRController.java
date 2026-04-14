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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Map<String, Integer> snVoteCount = new HashMap<>();

            for (OCRService.OcrResult candidate : ocrCandidates) {
                DeviceInfo info = extractorService.extractDeviceInfo(candidate.getText(), type);
                String normalizedSn = normalize(info.getSN());
                if (!normalizedSn.isEmpty()) {
                    snVoteCount.put(normalizedSn, snVoteCount.getOrDefault(normalizedSn, 0) + 1);
                }
                int score = scoreDeviceInfo(info);
                log.info("OCR候选提取结果: variant={}, score={}, info={}", candidate.getVariantName(), score, info);
                if (score > bestScore) {
                    bestScore = score;
                    bestInfo = info;
                }
            }

            if (bestInfo == null) {
                return R.ok(new DeviceInfo("", "", ""));
            }

            String votedSn = chooseVotedSn(snVoteCount);
            if (!votedSn.isEmpty() && !votedSn.equals(normalize(bestInfo.getSN()))) {
                bestInfo = new DeviceInfo(bestInfo.getIMEI(), bestInfo.getIMEI2(), votedSn);
            }
            return R.ok(bestInfo);
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            return R.fail("识别失败: " + e.getMessage());
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

        if (!sn.isEmpty()) {
            score += 100;
            if (sn.length() >= 10 && sn.length() <= 14) {
                score += 20;
            }
            if (sn.matches(".*[A-Z].*") && sn.matches(".*\\d.*")) {
                score += 20;
            }
        }
        if (!imei1.isEmpty()) {
            score += 40;
        }
        if (!imei2.isEmpty()) {
            score += 30;
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