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
            target.setActivated(obj.getActivated());
            target.setActivateDate(obj.getActivateDate());
        }
    }
}
