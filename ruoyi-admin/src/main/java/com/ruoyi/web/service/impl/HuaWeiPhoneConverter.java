package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.HuaWeiPhoneObject;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.PhoneInfoConverter;

public class HuaWeiPhoneConverter implements PhoneInfoConverter {

    @Override
    public void convert(Object source, PhoneInfoDto target) {
        if (source instanceof HuaWeiPhoneObject) {
            HuaWeiPhoneObject obj = (HuaWeiPhoneObject) source;
            target.setSn(obj.getSn());
            target.setModel(obj.getModel());
            target.setCoverage(obj.getCoverage());
            target.setActivated(obj.isActivated());
            target.setActivateDate(obj.getPurchase().getDate());
        }
    }
}
