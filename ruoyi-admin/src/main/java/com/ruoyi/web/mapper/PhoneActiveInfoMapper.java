package com.ruoyi.web.mapper;

import com.ruoyi.web.domain.PhoneActiveInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PhoneActiveInfoMapper {
    PhoneActiveInfo selectBySn(@Param("sn") String sn);
    int insert(PhoneActiveInfo info);
    int updateById(PhoneActiveInfo info);
    int deleteBySn(@Param("sn") String sn);

    /**
     * 逻辑删除订单（标记为测试数据，del_flag=2），仅管理员调用
     *
     * @param ids      订单ID集合（支持单条/多条）
     * @param updateBy 操作人
     * @return 实际更新的行数
     */
    int updateDelFlagByIds(@Param("ids") Long[] ids, @Param("updateBy") String updateBy);
    List<PhoneActiveInfo> selectByExample(PhoneActiveInfo phoneActiveInfo);

    /**
     * 列表查询（轻量：仅 JOIN 留资 + 部门，不 JOIN 详情表和签约表）
     */
    List<PhoneActiveInfo> selectListByExample(PhoneActiveInfo phoneActiveInfo);

    /**
     * 查询订单详情（全字段，JOIN 所有关联表）
     */
    PhoneActiveInfo selectDetailById(@Param("id") Long id);

    /**
     * 导出查询（全字段 JOIN 详情表和签约表，不分页）
     */
    List<PhoneActiveInfo> selectExportList(PhoneActiveInfo phoneActiveInfo);

    Map<String, Object> selectDashboardStatistics(@Param("currentDate") String currentDate,
                                                  @Param("previousDate") String previousDate,
                                                  @Param("currentMonth") String currentMonth,
                                                  @Param("previousMonth") String previousMonth,
                                                  @Param("storeId") Long storeId);

    Map<String, Object> selectUserStatistics(@Param("createBy") String createBy,
                                             @Param("currentDate") String currentDate,
                                             @Param("previousDate") String previousDate,
                                             @Param("recentSevenDaysStart") String recentSevenDaysStart,
                                             @Param("previousSevenDaysStart") String previousSevenDaysStart,
                                             @Param("previousSevenDaysEnd") String previousSevenDaysEnd);

    List<Map<String, Object>> selectOrderTrend(@Param("startDate") String startDate,
                                               @Param("endDate") String endDate,
                                               @Param("createBy") String createBy,
                                               @Param("storeId") Long storeId);

    List<Map<String, Object>> selectMonthlyOrderStats(@Param("startMonth") String startMonth,
                                                      @Param("endMonth") String endMonth,
                                                      @Param("storeId") Long storeId);

    List<Map<String, Object>> selectDeviceModelDistribution();

    List<Map<String, Object>> selectSignatureModelDistribution();

    Map<String, Object> selectWarrantyStatus(@Param("currentDate") String currentDate);

    List<PhoneActiveInfo> selectRecentOrders(@Param("createBy") String createBy, @Param("limit") int limit,
                                             @Param("storeId") Long storeId);

    /**
     * 查询门店列表（通信源手机部门的直接子部门）
     *
     * @param storeParentDeptId 门店父部门ID
     * @return 门店列表 {deptId, storeName}
     */
    List<Map<String, Object>> selectStoreList(@Param("storeParentDeptId") Long storeParentDeptId);

    /**
     * 按门店名称精确匹配门店ID（仅限门店层级，用于订单导入）
     *
     * @param storeName         门店名称
     * @param storeParentDeptId 门店父部门ID
     * @return 门店ID（sys_dept.dept_id），匹配不到返回 null
     */
    Long selectStoreIdByName(@Param("storeName") String storeName,
                             @Param("storeParentDeptId") Long storeParentDeptId);

    /**
     * 各门店订单对比统计（含无订单门店）
     *
     * @param storeParentDeptId 门店父部门ID
     * @return 门店统计列表 {deptId, storeName, totalOrders, activatedCount, signedCount}
     */
    List<Map<String, Object>> selectStoreComparison(@Param("storeParentDeptId") Long storeParentDeptId);

    /**
     * 根据订单ID查询签约时的协议内容快照
     *
     * @param id 订单ID
     * @return 协议内容（富文本HTML），可能为null
     */
    String selectContractContentById(@Param("id") Long id);
}