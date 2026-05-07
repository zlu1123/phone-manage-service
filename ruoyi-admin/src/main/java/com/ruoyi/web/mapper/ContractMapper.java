package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.Contract;
import com.ruoyi.web.domain.PhoneActiveInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 协议管理 Mapper 接口
 */
@Mapper
public interface ContractMapper {


    /**
     * 根据ID查询协议
     *
     * @param id 协议ID
     * @return 协议信息
     */
    Contract selectById(@Param("id") Integer id);

    /**
     * 根据协议名称查询
     *
     * @param name 协议名称
     * @return 协议信息
     */
    Contract selectByName(@Param("name") String name);

    /**
     * 根据版本号查询
     *
     * @param version 协议版本号
     * @return 协议信息
     */
    Contract selectByVersion(@Param("version") String version);

    /**
     * 查询所有协议列表
     *
     * @return 协议列表
     */
    List<Contract> selectAll();

    /**
     * 查询生效中的协议列表
     *
     * @return 生效中的协议列表
     */
    List<Contract> selectActiveContracts();

    /**
     * 分页查询协议列表
     *
     * @param offset 起始位置
     * @param limit  查询数量
     * @return 协议列表
     */
    List<Contract> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 条件查询协议列表
     *
     * @param contract 查询条件
     * @return 协议列表
     */
    List<Contract> selectByCondition(Contract contract);

    /**
     * 新增协议
     *
     * @param contract 协议信息
     * @return 影响行数
     */
    int insert(Contract contract);

    /**
     * 选择性新增协议（只插入非空字段）
     *
     * @param contract 协议信息
     * @return 影响行数
     */
    int insertSelective(Contract contract);

    /**
     * 根据ID更新协议
     *
     * @param contract 协议信息
     * @return 影响行数
     */
    int updateById(Contract contract);

    /**
     * 选择性更新协议（只更新非空字段）
     *
     * @param contract 协议信息
     * @return 影响行数
     */
    int updateSelective(Contract contract);

    /**
     * 更新协议状态
     *
     * @param id     协议ID
     * @param status 状态值
     * @return 影响行数
     */
    int updateStatus(@Param("id") Integer id, @Param("status") Boolean status);

    /**
     * 根据ID删除协议
     *
     * @param id 协议ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 批量删除协议
     *
     * @param ids 协议ID数组
     * @return 影响行数
     */
    int deleteBatch(@Param("ids") Integer[] ids);

    /**
     * 统计协议总数
     *
     * @return 协议总数
     */
    int countTotal();

    /**
     * 检查协议名称是否存在
     *
     * @param name 协议名称
     * @return 存在数量
     */
    int checkNameExists(@Param("name") String name);

    /**
     * 检查协议版本是否存在
     *
     * @param version 协议版本号
     * @return 存在数量
     */
    int checkVersionExists(@Param("version") String version);
}