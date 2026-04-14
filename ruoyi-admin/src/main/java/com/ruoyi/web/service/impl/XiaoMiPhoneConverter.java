package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.model.XiaoMiPhoneObject;
import com.ruoyi.web.service.PhoneInfoConverter;

public class XiaoMiPhoneConverter implements PhoneInfoConverter {

    @Override
    public void convert(Object source, PhoneInfoDto target) {
        if (source instanceof XiaoMiPhoneObject) {
            XiaoMiPhoneObject obj = (XiaoMiPhoneObject) source;
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
