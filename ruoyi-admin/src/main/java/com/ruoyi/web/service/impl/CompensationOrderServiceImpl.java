package com.ruoyi.web.service.impl;

import com.ruoyi.web.domain.CompensationOrder;
import com.ruoyi.web.mapper.CompensationOrderMapper;
import com.ruoyi.web.model.CompensationOrderDto;
import com.ruoyi.web.model.CompensationOrderReturnDto;
import com.ruoyi.web.service.CompensationOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 赔付订单管理 Service 实现类
 */
@Service
public class CompensationOrderServiceImpl implements CompensationOrderService {

    @Autowired
    private CompensationOrderMapper compensationOrderMapper;

    /**
     * 新增
     * @param compensationOrder 信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(CompensationOrder compensationOrder) {

        // 校验同一订单是否已有待审核的赔付记录
        if (compensationOrder.getOrderId() != null) {
            checkPendingCompensationExists(compensationOrder.getOrderId());
        }

        // 设置创建时间和更新时间
        Date now = new Date();
        compensationOrder.setCreateTime(now);
        compensationOrder.setUpdateTime(now);

        // 执行插入操作
        compensationOrderMapper.insertSelective(compensationOrder);

        System.out.println("生成的主键ID: " + compensationOrder.getId());
        return compensationOrder.getId();
    }

    /**
     * 更新赔付订单
     * @param compensationOrder 赔付订单信息
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(CompensationOrder compensationOrder) {
        // 参数校验
        if (compensationOrder == null) {
            throw new IllegalArgumentException("赔付订单不能为空");
        }
        if (compensationOrder.getId() == null) {
            throw new IllegalArgumentException("赔付订单ID不能为空");
        }

        // 查询原赔付订单信息
        CompensationOrder oldLeaveInformation = compensationOrderMapper.selectById(compensationOrder.getId());
        if (oldLeaveInformation == null) {
            throw new RuntimeException("赔付订单不存在，ID：" + compensationOrder.getId());
        }

        // 设置更新时间
        compensationOrder.setUpdateTime(new Date());

        // 执行更新操作（只更新非空字段）
        return compensationOrderMapper.updateSelective(compensationOrder);
    }

    /**
     * 删除赔付订单
     * @param compensationOrder 赔付订单信息（包含ID）
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(CompensationOrder compensationOrder) {
        // 参数校验
        if (compensationOrder == null) {
            throw new IllegalArgumentException("赔付订单不能为空");
        }
        if (compensationOrder.getId() == null) {
            throw new IllegalArgumentException("赔付订单ID不能为空");
        }

        // 查询赔付订单是否存在
        CompensationOrder leaveInformation1 = compensationOrderMapper.selectById(compensationOrder.getId());
        if (leaveInformation1 == null) {
            throw new RuntimeException("袖子信息不存在，ID：" + compensationOrder.getId());
        }

        // 执行删除操作
        return compensationOrderMapper.deleteById(compensationOrder.getId());
    }


    /**
     * 条件查询赔付订单列表
     * @param CompensationOrder 查询条件
     * @return 赔付订单列表
     */
    public List<CompensationOrderReturnDto> queryCompensationOrderByCondition(CompensationOrderDto compensationOrderDto) {
        return compensationOrderMapper.selectByCondition(compensationOrderDto);
    }

    /**
     * 校验指定订单是否有待审核的赔付记录
     *
     * @param orderId 订单ID
     * @throws RuntimeException 如果存在待审核的赔付订单
     */
    @Override
    public void checkPendingCompensationExists(Long orderId) {
        CompensationOrder pendingOrder = compensationOrderMapper.selectByOrderIdAndStatus(orderId, 0);
        if (pendingOrder != null) {
            throw new RuntimeException("该订单已存在待审核的赔付记录，不可重复发起赔付");
        }
    }

}