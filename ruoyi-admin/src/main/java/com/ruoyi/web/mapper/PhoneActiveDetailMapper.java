package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.PhoneActiveDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单详情 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface PhoneActiveDetailMapper {

    /**
     * 新增订单详情
     *
     * @param detail 订单详情
     * @return 影响行数
     */
    int insert(PhoneActiveDetail detail);
}
