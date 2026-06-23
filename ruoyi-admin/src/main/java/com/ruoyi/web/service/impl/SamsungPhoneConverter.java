package com.ruoyi.web.service.impl;

import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.model.SamsungPhoneObject;
import com.ruoyi.web.service.PhoneInfoConverter;

public class SamsungPhoneConverter implements PhoneInfoConverter {

    @Override
    public void convert(Object source, PhoneInfoDto target) {
        if (source instanceof SamsungPhoneObject) {
            SamsungPhoneObject obj = (SamsungPhoneObject) source;
            // 三星 API 可能返回 serial 或 sn，优先使用 sn
            target.setSn(obj.getSn() != null ? obj.getSn() : obj.getSerial());
            target.setImei1(obj.getImei1());
            target.setImei2(obj.getImei2());
            target.setModel(obj.getModel());
            target.setCoverage(obj.getCoverage());
            target.setActivated(obj.getActivated());
            target.setActivateDate(obj.getActivateDate());
        }
    }
}
