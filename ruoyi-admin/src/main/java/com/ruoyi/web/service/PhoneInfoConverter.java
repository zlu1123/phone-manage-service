package com.ruoyi.web.service;


import com.ruoyi.web.model.PhoneInfoDto;

public interface PhoneInfoConverter {
    /**
     * 将源对象转换为 PhoneInfoDto
     * @param source 源对象（具体类型由实现类决定）
     * @param target 目标 DTO（已创建，只填充属性）
     */
    void convert(Object source, PhoneInfoDto target);
}