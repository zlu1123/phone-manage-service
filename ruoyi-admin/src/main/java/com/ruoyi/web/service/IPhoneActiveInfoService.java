package com.ruoyi.web.service;

import com.ruoyi.web.domain.PhoneActiveInfo;

import java.util.List;
import java.util.Map;

public interface IPhoneActiveInfoService {
    Long saveOrUpdateActiveInfo(PhoneActiveInfo info);

    List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info);

    Map<String, Object> getDashboardStatistics(String currentDate);

    Map<String, Object> getUserDashboardStatistics(String createBy, String currentDate);

    Map<String, Object> getOrderTrend(String period, String createBy, String currentDate);

    List<Map<String, Object>> getDeviceModelDistribution();

    Map<String, Object> getMonthlyOrderStats(String currentDate);

    List<Map<String, Object>> getWarrantyStatus(String currentDate);

    List<PhoneActiveInfo> getRecentOrders(String createBy, int limit);
}