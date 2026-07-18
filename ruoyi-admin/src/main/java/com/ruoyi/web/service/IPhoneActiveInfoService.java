package com.ruoyi.web.service;

import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.domain.PhoneOrderContract;

import java.util.List;
import java.util.Map;

public interface IPhoneActiveInfoService {
    Long saveOrUpdateActiveInfo(PhoneActiveInfo info);

    /**
     * 保存或更新签约信息
     *
     * @param contract 签约信息（必须包含 orderId）
     */
    void saveOrUpdateContract(PhoneOrderContract contract);

    List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info);

    /**
     * 查询订单详情（全字段，包含详情表和签约表）
     *
     * @param id 订单ID
     * @return 订单详情，可能为null
     */
    PhoneActiveInfo getActiveDetail(Long id);

    /**
     * 导出查询（全字段，不分页）
     */
    List<PhoneActiveInfo> queryExportList(PhoneActiveInfo info);

    /**
     * 批量导入订单数据
     *
     * @param list 导入的数据列表
     * @param updateSupport 是否更新已存在的数据（根据sn判断）
     * @param operName 操作人
     * @return 导入结果消息
     */
    String importActiveInfo(List<PhoneActiveInfo> list, boolean updateSupport, String operName);

    Map<String, Object> getDashboardStatistics(String currentDate);

    Map<String, Object> getUserDashboardStatistics(String createBy, String currentDate);

    Map<String, Object> getOrderTrend(String period, String createBy, String currentDate);

    List<Map<String, Object>> getDeviceModelDistribution();

    List<Map<String, Object>> getSignatureModelDistribution();

    Map<String, Object> getMonthlyOrderStats(String currentDate);

    List<Map<String, Object>> getWarrantyStatus(String currentDate);

    List<PhoneActiveInfo> getRecentOrders(String createBy, int limit);

    /**
     * 根据订单ID查询签约时的协议内容（历史快照，不受协议模板表变更影响）
     *
     * @param id 订单ID
     * @return 协议内容富文本，可能为null
     */
    String getContractContent(Long id);

    /**
     * 根据用户名解析其所属门店ID
     * 从用户的部门出发，向上查找通信源手机 (201) 的直接子部门即为门店
     *
     * @param username 用户名
     * @return 门店ID (sys_dept.dept_id)，null表示无法解析
     */
    Long resolveStoreIdByUsername(String username);
}