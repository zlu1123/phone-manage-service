package com.ruoyi.web.service;

import com.ruoyi.web.domain.PhoneActiveInfo;

import java.util.List;
import java.util.Map;

public interface IPhoneActiveInfoService {
    Long saveOrUpdateActiveInfo(PhoneActiveInfo info);

    List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info);

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
}