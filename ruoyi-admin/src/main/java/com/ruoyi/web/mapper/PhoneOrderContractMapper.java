package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.PhoneOrderContract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单签约信息 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface PhoneOrderContractMapper {

    /**
     * 根据订单ID查询签约信息
     *
     * @param orderId 订单ID
     * @return 签约信息
     */
    PhoneOrderContract selectByOrderId(Long orderId);

    /**
     * 新增签约信息
     *
     * @param contract 签约信息
     * @return 影响行数
     */
    int insert(PhoneOrderContract contract);

    /**
     * 根据订单ID更新签约信息
     *
     * @param contract 签约信息
     * @return 影响行数
     */
    int updateByOrderId(PhoneOrderContract contract);

    /**
     * 根据订单ID更新签约信息（仅更新非空字段，用于导入更新时避免覆盖未填写的列）
     *
     * @param contract 签约信息（必须包含 orderId）
     * @return 影响行数
     */
    int updateByOrderIdSelective(PhoneOrderContract contract);
}
