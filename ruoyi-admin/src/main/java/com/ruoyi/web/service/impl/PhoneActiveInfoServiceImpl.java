package com.ruoyi.web.service.impl;

import com.ruoyi.web.mapper.PhoneActiveInfoMapper;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class PhoneActiveInfoServiceImpl implements IPhoneActiveInfoService {

    @Autowired
    private PhoneActiveInfoMapper phoneActiveInfoMapper;

    @Override
    @Transactional
    public void saveOrUpdateActiveInfo(PhoneActiveInfo info) {
        if (info == null || info.getSn() == null) {
            return;
        }
        PhoneActiveInfo exist = phoneActiveInfoMapper.selectBySn(info.getSn());
        if (exist != null) {
            info.setId(exist.getId());
            info.setCreateTime(exist.getCreateTime());
            info.setCreateBy(exist.getCreateBy());
            info.setUpdateTime(new Date());
            phoneActiveInfoMapper.updateById(info);
        } else {
            info.setCreateTime(new Date());
            info.setUpdateTime(new Date());
            phoneActiveInfoMapper.insert(info);
        }
    }

    @Override
    public List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info) {
        return phoneActiveInfoMapper.selectByExample(info);
    }
}