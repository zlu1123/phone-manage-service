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
            String ocrText = ocrService.recognize(file);
            DeviceInfo info = extractorService.extractDeviceInfo(ocrText, type);
            return R.ok(info);
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            return R.fail("识别失败: " + e.getMessage());
        }
    }
}