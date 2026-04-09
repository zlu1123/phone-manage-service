package com.ruoyi.web.service;

import com.ruoyi.web.domain.PhoneActiveInfo;

import java.util.List;

public interface IPhoneActiveInfoService {
    void saveOrUpdateActiveInfo(PhoneActiveInfo info);

    List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info);
}