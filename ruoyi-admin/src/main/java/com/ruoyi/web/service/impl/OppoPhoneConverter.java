package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.OppoPhoneObject;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.PhoneInfoConverter;

public class OppoPhoneConverter implements PhoneInfoConverter {

    @Override
    public void convert(Object source, PhoneInfoDto target) {
        if (source instanceof OppoPhoneObject) {
            OppoPhoneObject obj = (OppoPhoneObject) source;
            target.setSn(obj.getSn());
            target.setImei1(obj.getImei1());
            target.setImei2(obj.getImei2());
            target.setModel(obj.getModel());
            target.setCoverage(obj.getCoverage());
            // OPPO API 不返回 activated 布尔值，通过 purchase.date 判断是否激活
            if (obj.getPurchase() != null && obj.getPurchase().getDate() != null && !obj.getPurchase().getDate().isEmpty()) {
                target.setActivated(true);
                target.setActivateDate(obj.getPurchase().getDate());
            } else {
                target.setActivated(false);
                target.setActivateDate(null);
            }
        }
    }
}
