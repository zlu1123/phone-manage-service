package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.LeaveInformation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 留资信息管理 Mapper 接口
 */
@Mapper
public interface LeaveInformationMapper {


    /**
     * 根据ID查询留资信息
     *
     * @param id 留资ID
     * @return 留资信息
     */
    LeaveInformation selectById(@Param("id") Long id);


    /**
     * 查询所有留资信息列表
     *
     * @return 留资列表
     */
    List<LeaveInformation> selectAll();

    /**
     * 分页查询留资信息列表
     *
     * @param offset 起始位置
     * @param limit  查询数量
     * @return 留资信息列表
     */
    List<LeaveInformation> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 条件查询留资信息列表
     *
     * @param LeaveInformation 查询条件
     * @return 留资信息列表
     */
    List<LeaveInformation> selectByCondition(LeaveInformation leaveInformation);

    List<LeaveInformation> selectByNameOrNum(@Param("context") String context);

    /**
     * 新增留资信息
     *
     * @param LeaveInformation 留资信息
     * @return 影响行数
     */
    int insert(LeaveInformation leaveInformation);

    /**
     * 选择性新增留资信息（只插入非空字段）
     *
     * @param LeaveInformation 留资信息
     * @return 影响行数
     */
    int insertSelective(LeaveInformation leaveInformation);

    /**
     * 根据ID更新
     *
     * @param LeaveInformation 信息
     * @return 影响行数
     */
    int updateById(LeaveInformation leaveInformation);

    /**
     * 选择性更新（只更新非空字段）
     *
     * @param LeaveInformation 信息
     * @return 影响行数
     */
    int updateSelective(LeaveInformation leaveInformation);

    /**
     * 根据ID删除
     *
     * @param id ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);


    /**
     * 统计总数
     *
     * @return 总数
     */
    int countTotal();

    /**
     * 检查电话号码否存在
     *
     * @param phoneNum 电话号码
     * @return 存在数量
     */
    int checkPhoneNumExists(@Param("phoneNum") String phoneNum);
}