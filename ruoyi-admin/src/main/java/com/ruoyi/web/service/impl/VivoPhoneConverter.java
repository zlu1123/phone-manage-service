package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.model.VivoPhoneObject;
import com.ruoyi.web.service.PhoneInfoConverter;

public class VivoPhoneConverter implements PhoneInfoConverter {

    @Override
    public void convert(Object source, PhoneInfoDto target) {
        if (source instanceof VivoPhoneObject) {
            VivoPhoneObject obj = (VivoPhoneObject) source;
            target.setSn(obj.getSn());
            target.setImei1(obj.getImei1());
            target.setImei2(obj.getImei2());
            target.setModel(obj.getModel());
            target.setCoverage(obj.getCoverage());
            // Vivo API 的 activated 返回的是激活日期字符串（如"2025年10月17日"），非布尔值
            String activatedDateStr = obj.getActivated();
            target.setActivated(activatedDateStr != null && !activatedDateStr.isEmpty());
            // 如果 activateDate 为空，用 activated 的日期字符串兜底
            if (obj.getActivateDate() != null && !obj.getActivateDate().isEmpty()) {
                target.setActivateDate(obj.getActivateDate());
            } else {
                target.setActivateDate(activatedDateStr);
            }
        }
    }
}
