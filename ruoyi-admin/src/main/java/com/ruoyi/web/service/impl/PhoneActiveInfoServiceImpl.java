package com.ruoyi.web.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.mapper.PhoneActiveInfoMapper;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PhoneActiveInfoServiceImpl implements IPhoneActiveInfoService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private PhoneActiveInfoMapper phoneActiveInfoMapper;

    @Autowired
    private ServerConfig serverConfig;

    @Override
    @Transactional
    public Long saveOrUpdateActiveInfo(PhoneActiveInfo info) {
        if (info == null) {
            return null;
        }
        if (info.getId() != null) {
            info.setUpdateTime(new Date());
            int i = phoneActiveInfoMapper.updateById(info);
            return (long) i;
        }
        info.setImagePath(normalizeImagePath(info.getImagePath()));
        info.setCreateTime(new Date());
        info.setUpdateTime(new Date());
        phoneActiveInfoMapper.insert(info);
        System.out.println("生成的主键ID: " + info.getId());
        return info.getId();
    }

    @Override
    public List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info) {
        return fillImageUrls(phoneActiveInfoMapper.selectByExample(info));
    }

    @Override
    public Map<String, Object> getDashboardStatistics(String currentDate) {
        LocalDate businessDate = parseDate(currentDate);
        String previousDate = businessDate.minusDays(1).format(DAY_FORMATTER);
        String currentMonth = businessDate.format(MONTH_FORMATTER);
        String previousMonth = businessDate.minusMonths(1).format(MONTH_FORMATTER);

        Map<String, Object> result = phoneActiveInfoMapper.selectDashboardStatistics(
                businessDate.format(DAY_FORMATTER), previousDate, currentMonth, previousMonth);
        long totalOrders = getLongValue(result, "totalOrders");
        long todayOrders = getLongValue(result, "todayOrders");
        long activatedDevices = getLongValue(result, "activatedDevices");
        long pendingOrders = getLongValue(result, "pendingOrders");
        long yesterdayOrders = getLongValue(result, "yesterdayOrders");
        long currentMonthOrders = getLongValue(result, "currentMonthOrders");
        long previousMonthOrders = getLongValue(result, "previousMonthOrders");

        Map<String, Object> statistics = new HashMap<>(16);
        statistics.put("totalOrders", totalOrders);
        statistics.put("todayOrders", todayOrders);
        statistics.put("activatedDevices", activatedDevices);
        statistics.put("pendingOrders", pendingOrders);
        statistics.put("totalOrdersGrowthRate", calculateGrowthRate(currentMonthOrders, previousMonthOrders));
        statistics.put("todayOrdersGrowthRate", calculateGrowthRate(todayOrders, yesterdayOrders));
        statistics.put("activationRate", calculateRatio(activatedDevices, totalOrders));
        statistics.put("pendingRate", calculateRatio(pendingOrders, totalOrders));
        statistics.put("totalOrdersGrowthLabel", buildTrendLabel(calculateGrowthRate(currentMonthOrders, previousMonthOrders), "较上月"));
        statistics.put("todayOrdersGrowthLabel", buildTrendLabel(calculateGrowthRate(todayOrders, yesterdayOrders), "较昨日"));
        statistics.put("activationRateLabel", buildRateLabel(calculateRatio(activatedDevices, totalOrders), "激活率"));
        statistics.put("pendingRateLabel", buildRateLabel(calculateRatio(pendingOrders, totalOrders), "待处理占比"));
        statistics.put("totalOrdersTrend", calculateGrowthRate(currentMonthOrders, previousMonthOrders) >= 0 ? "up" : "down");
        statistics.put("todayOrdersTrend", calculateGrowthRate(todayOrders, yesterdayOrders) >= 0 ? "up" : "down");
        statistics.put("activationRateTrend", calculateRatio(activatedDevices, totalOrders) >= 0 ? "up" : "down");
        statistics.put("pendingRateTrend", calculateRatio(pendingOrders, totalOrders) <= 50 ? "down" : "up");
        return statistics;
    }

    @Override
    public Map<String, Object> getUserDashboardStatistics(String createBy, String currentDate) {
        LocalDate businessDate = parseDate(currentDate);
        String previousDate = businessDate.minusDays(1).format(DAY_FORMATTER);
        String recentSevenDaysStart = businessDate.minusDays(6).format(DAY_FORMATTER);
        String previousSevenDaysStart = businessDate.minusDays(13).format(DAY_FORMATTER);
        String previousSevenDaysEnd = businessDate.minusDays(7).format(DAY_FORMATTER);

        Map<String, Object> result = phoneActiveInfoMapper.selectUserStatistics(
                createBy,
                businessDate.format(DAY_FORMATTER),
                previousDate,
                recentSevenDaysStart,
                previousSevenDaysStart,
                previousSevenDaysEnd);
        long myTotalOrders = getLongValue(result, "myTotalOrders");
        long myTodayOrders = getLongValue(result, "myTodayOrders");
        long myActivatedDevices = getLongValue(result, "myActivatedDevices");
        long myPendingOrders = getLongValue(result, "myPendingOrders");
        long myYesterdayOrders = getLongValue(result, "myYesterdayOrders");
        long myRecentSevenDaysOrders = getLongValue(result, "myRecentSevenDaysOrders");
        long myPreviousSevenDaysOrders = getLongValue(result, "myPreviousSevenDaysOrders");

        double myTotalOrdersGrowthRate = calculateGrowthRate(myRecentSevenDaysOrders, myPreviousSevenDaysOrders);
        double myTodayOrdersGrowthRate = calculateGrowthRate(myTodayOrders, myYesterdayOrders);
        double myActivationRate = calculateRatio(myActivatedDevices, myTotalOrders);
        double myPendingRate = calculateRatio(myPendingOrders, myTotalOrders);

        Map<String, Object> statistics = new HashMap<>(12);
        statistics.put("myTotalOrders", myTotalOrders);
        statistics.put("myTodayOrders", myTodayOrders);
        statistics.put("myActivatedDevices", myActivatedDevices);
        statistics.put("myPendingOrders", myPendingOrders);
        statistics.put("myTotalOrdersGrowthLabel", buildTrendLabel(myTotalOrdersGrowthRate, "近7天较前7天"));
        statistics.put("myTodayOrdersGrowthLabel", buildTrendLabel(myTodayOrdersGrowthRate, "较昨日"));
        statistics.put("myActivationRateLabel", buildRateLabel(myActivationRate, "激活率"));
        statistics.put("myPendingRateLabel", buildRateLabel(myPendingRate, "待处理占比"));
        statistics.put("myTotalOrdersTrend", myTotalOrdersGrowthRate >= 0 ? "up" : "down");
        statistics.put("myTodayOrdersTrend", myTodayOrdersGrowthRate >= 0 ? "up" : "down");
        statistics.put("myActivationRateTrend", myActivationRate >= 50 ? "up" : "down");
        statistics.put("myPendingRateTrend", myPendingRate <= 50 ? "down" : "up");
        return statistics;
    }

    @Override
    public Map<String, Object> getOrderTrend(String period, String createBy, String currentDate) {
        int days = "month".equalsIgnoreCase(period) ? 30 : 7;
        LocalDate endDate = parseDate(currentDate);
        LocalDate startDate = endDate.minusDays(days - 1L);
        List<Map<String, Object>> rows = phoneActiveInfoMapper.selectOrderTrend(
                startDate.format(DAY_FORMATTER),
                endDate.format(DAY_FORMATTER),
                createBy);

        Map<String, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            rowMap.put(String.valueOf(row.get("day")), row);
        }

        List<String> dates = new ArrayList<>();
        List<Long> newOrders = new ArrayList<>();
        List<Long> completedOrders = new ArrayList<>();
        List<Long> myOrders = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate day = startDate.plusDays(i);
            String fullDate = day.format(DAY_FORMATTER);
            String displayDate = day.format(DateTimeFormatter.ofPattern("MM-dd"));
            Map<String, Object> row = rowMap.get(fullDate);
            long orderCount = getLongValue(row, "orderCount");
            long activatedCount = getLongValue(row, "activatedCount");
            dates.add(displayDate);
            newOrders.add(orderCount);
            completedOrders.add(activatedCount);
            myOrders.add(orderCount);
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("dates", dates);
        result.put("newOrders", newOrders);
        result.put("completedOrders", completedOrders);
        result.put("myOrders", myOrders);
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeviceModelDistribution() {
        List<Map<String, Object>> result = phoneActiveInfoMapper.selectDeviceModelDistribution();
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public Map<String, Object> getMonthlyOrderStats(String currentDate) {
        LocalDate businessDate = parseDate(currentDate);
        YearMonth currentMonth = YearMonth.from(businessDate);
        YearMonth startMonth = currentMonth.minusMonths(5);
        List<Map<String, Object>> rows = phoneActiveInfoMapper.selectMonthlyOrderStats(
                startMonth.format(MONTH_FORMATTER),
                currentMonth.format(MONTH_FORMATTER));

        Map<String, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            rowMap.put(String.valueOf(row.get("month")), row);
        }

        List<String> months = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Long> activatedCounts = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            YearMonth month = startMonth.plusMonths(i);
            String key = month.format(MONTH_FORMATTER);
            Map<String, Object> row = rowMap.get(key);
            months.add(key);
            orderCounts.add(getLongValue(row, "orderCount"));
            activatedCounts.add(getLongValue(row, "activatedCount"));
        }

        Map<String, Object> result = new HashMap<>(3);
        result.put("months", months);
        result.put("orderCounts", orderCounts);
        result.put("activatedCounts", activatedCounts);
        return result;
    }

    @Override
    public List<Map<String, Object>> getWarrantyStatus(String currentDate) {
        Map<String, Object> row = phoneActiveInfoMapper.selectWarrantyStatus(currentDate);
        List<Map<String, Object>> result = new ArrayList<>(3);
        result.add(buildNameValue("保修期内", getLongValue(row, "inWarrantyCount")));
        result.add(buildNameValue("即将过保（30天内）", getLongValue(row, "nearExpiryCount")));
        result.add(buildNameValue("已过保", getLongValue(row, "expiredCount")));
        return result;
    }

    @Override
    public List<PhoneActiveInfo> getRecentOrders(String createBy, int limit) {
        return fillImageUrls(phoneActiveInfoMapper.selectRecentOrders(createBy, limit));
    }

    private List<PhoneActiveInfo> fillImageUrls(List<PhoneActiveInfo> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        for (PhoneActiveInfo item : list) {
            fillImageUrl(item);
        }
        return list;
    }

    private void fillImageUrl(PhoneActiveInfo info) {
        if (info == null) {
            return;
        }
        String normalizedImagePath = normalizeImagePath(info.getImagePath());
        info.setImagePath(normalizedImagePath);
        info.setImageUrl(buildImageUrl(normalizedImagePath));
    }

    private String normalizeImagePath(String imagePath) {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        List<String> normalizedPaths = new ArrayList<>();
        for (String item : imagePath.split(",")) {
            String normalizedPath = normalizeSingleImagePath(item);
            if (!StringUtils.isEmpty(normalizedPath)) {
                normalizedPaths.add(normalizedPath);
            }
        }
        return normalizedPaths.isEmpty() ? null : String.join(",", normalizedPaths);
    }

    private String normalizeSingleImagePath(String imagePath) {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        String trimmedPath = imagePath.trim();
        if (trimmedPath.isEmpty()) {
            return null;
        }
        int profileIndex = trimmedPath.indexOf("/profile/");
        if (profileIndex >= 0) {
            return trimmedPath.substring(profileIndex);
        }
        return trimmedPath;
    }

    private String buildImageUrl(String imagePath) {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        List<String> imageUrls = new ArrayList<>();
        for (String item : imagePath.split(",")) {
            String imageUrl = buildSingleImageUrl(item);
            if (!StringUtils.isEmpty(imageUrl)) {
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls.isEmpty() ? null : String.join(",", imageUrls);
    }

    private String buildSingleImageUrl(String imagePath) {
        String normalizedImagePath = normalizeSingleImagePath(imagePath);
        if (StringUtils.isEmpty(normalizedImagePath)) {
            return null;
        }
        if (isExternalUrl(normalizedImagePath)) {
            return normalizedImagePath;
        }
        try {
            return serverConfig.getUrl() + normalizedImagePath;
        } catch (Exception e) {
            return normalizedImagePath;
        }
    }

    private boolean isExternalUrl(String value) {
        String lowerValue = value.toLowerCase();
        return lowerValue.startsWith("http://") || lowerValue.startsWith("https://");
    }

    private Map<String, Object> buildNameValue(String name, long value) {
        Map<String, Object> item = new LinkedHashMap<>(2);
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private LocalDate parseDate(String currentDate) {
        try {
            return LocalDate.parse(currentDate, DAY_FORMATTER);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private double calculateGrowthRate(long currentValue, long previousValue) {
        if (previousValue == 0L) {
            return currentValue > 0L ? 100D : 0D;
        }
        return ((double) (currentValue - previousValue) / (double) previousValue) * 100D;
    }

    private double calculateRatio(long numerator, long denominator) {
        if (denominator == 0L) {
            return 0D;
        }
        return ((double) numerator / (double) denominator) * 100D;
    }

    private String buildTrendLabel(double value, String suffix) {
        return formatPercent(value) + " " + suffix;
    }

    private String buildRateLabel(double value, String prefix) {
        return prefix + " " + formatPercent(value);
    }

    private String formatPercent(double value) {
        return String.format("%.1f%%", Math.abs(value));
    }

    private long getLongValue(Map<String, Object> row, String key) {
        if (row == null || row.get(key) == null) {
            return 0L;
        }
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}