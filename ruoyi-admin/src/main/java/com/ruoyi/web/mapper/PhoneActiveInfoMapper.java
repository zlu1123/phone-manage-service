package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.PhoneActiveInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PhoneActiveInfoMapper {
    PhoneActiveInfo selectBySn(@Param("sn") String sn);
    int insert(PhoneActiveInfo info);
    int updateById(PhoneActiveInfo info);
    int deleteBySn(@Param("sn") String sn);
    List<PhoneActiveInfo> selectByExample(PhoneActiveInfo phoneActiveInfo);
}