package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.CompensationOrder;
import com.ruoyi.web.model.CompensationOrderDto;
import com.ruoyi.web.model.CompensationOrderReturnDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 赔付订单管理 Mapper 接口
 */
@Mapper
public interface CompensationOrderMapper {


    /**
     * 根据ID查询赔付订单
     *
     * @param id 赔付ID
     * @return 赔付订单
     */
    CompensationOrder selectById(@Param("id") Long id);

    /**
     * 条件查询赔付订单列表
     *
     * @param compensationOrderDto 查询条件
     * @return 赔付订单列表
     */
    List<CompensationOrderReturnDto> selectByCondition(CompensationOrderDto compensationOrderDto);

    /**
     * 新增赔付订单
     *
     * @param compensationOrder 赔付订单
     * @return 影响行数
     */
    int insert(CompensationOrder compensationOrder);

    /**
     * 选择性新增赔付订单（只插入非空字段）
     *
     * @param compensationOrder 赔付订单
     * @return 影响行数
     */
    int insertSelective(CompensationOrder compensationOrder);

    /**
     * 选择性更新（只更新非空字段）
     *
     * @param compensationOrder 信息
     * @return 影响行数
     */
    int updateSelective(CompensationOrder compensationOrder);

    /**
     * 根据ID删除
     *
     * @param id ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}