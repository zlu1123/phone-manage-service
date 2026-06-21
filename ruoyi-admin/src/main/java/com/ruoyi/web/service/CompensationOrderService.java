package com.ruoyi.web.service;

import com.ruoyi.web.domain.CompensationOrder;
import com.ruoyi.web.model.CompensationOrderDto;
import com.ruoyi.web.model.CompensationOrderReturnDto;

import java.util.List;

public interface CompensationOrderService {

    Long insert(CompensationOrder compensationOrder);

    int update(CompensationOrder compensationOrder);

    int delete(CompensationOrder compensationOrder);

    List<CompensationOrderReturnDto> queryCompensationOrderByCondition(CompensationOrderDto compensationOrderDto);

    /**
     * 校验指定订单是否有待审核的赔付记录，如有则抛出异常
     *
     * @param orderId 订单ID
     * @throws RuntimeException 如果存在待审核的赔付订单
     */
    void checkPendingCompensationExists(Long orderId);

}
