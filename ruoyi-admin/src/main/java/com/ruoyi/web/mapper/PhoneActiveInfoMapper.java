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
    List<PhoneActiveInfo> selectByExample(PhoneActiveInfo phoneActiveInfo);

    Map<String, Object> selectDashboardStatistics(@Param("currentDate") String currentDate,
                                                  @Param("previousDate") String previousDate,
                                                  @Param("currentMonth") String currentMonth,
                                                  @Param("previousMonth") String previousMonth);

    Map<String, Object> selectUserStatistics(@Param("createBy") String createBy,
                                             @Param("currentDate") String currentDate,
                                             @Param("previousDate") String previousDate,
                                             @Param("recentSevenDaysStart") String recentSevenDaysStart,
                                             @Param("previousSevenDaysStart") String previousSevenDaysStart,
                                             @Param("previousSevenDaysEnd") String previousSevenDaysEnd);

    List<Map<String, Object>> selectOrderTrend(@Param("startDate") String startDate,
                                               @Param("endDate") String endDate,
                                               @Param("createBy") String createBy);

    List<Map<String, Object>> selectMonthlyOrderStats(@Param("startMonth") String startMonth,
                                                      @Param("endMonth") String endMonth);

    List<Map<String, Object>> selectDeviceModelDistribution();

    Map<String, Object> selectWarrantyStatus(@Param("currentDate") String currentDate);

    List<PhoneActiveInfo> selectRecentOrders(@Param("createBy") String createBy, @Param("limit") int limit);

    /**
     * 根据订单ID查询签约时的协议内容快照
     *
     * @param id 订单ID
     * @return 协议内容（富文本HTML），可能为null
     */
    String selectContractContentById(@Param("id") Long id);
}