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

}
