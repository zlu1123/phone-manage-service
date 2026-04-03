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
            target.setActivateDate(obj.getEstPurchaseDate());
        }
    }
}
