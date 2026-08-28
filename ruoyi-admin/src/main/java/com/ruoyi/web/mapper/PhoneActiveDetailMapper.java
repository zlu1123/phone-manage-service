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

    /**
     * 按订单ID更新订单详情（仅更新非空字段，用于导入更新时避免覆盖未填写的列）
     *
     * @param detail 订单详情（必须包含 orderId）
     * @return 影响行数
     */
    int updateByOrderIdSelective(PhoneActiveDetail detail);
}
