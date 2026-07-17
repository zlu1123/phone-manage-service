package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.ApplePhoneObject;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.PhoneInfoConverter;

public class ApplePhoneConverter implements PhoneInfoConverter {

    @Override
    public void convert(Object source, PhoneInfoDto target) {
        if (source instanceof ApplePhoneObject) {
            ApplePhoneObject obj = (ApplePhoneObject) source;
            target.setSn(obj.getSerial());
            target.setModel(obj.getModel());
            target.setCoverage(obj.getRepairExpiry());
            target.setActivated(obj.getActivated());
            // 优先取 activeDate（真正的激活日期），不返回时兜底 estPurchaseDate
            String activateDate = obj.getActiveDate() != null ? obj.getActiveDate() : obj.getEstPurchaseDate();
            target.setActivateDate(activateDate);
        }
    }
}
