package com.ruoyi.web.service.impl;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.web.domain.PhoneActiveDetail;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.domain.PhoneOrderContract;
import com.ruoyi.web.domain.LeaveInformation;
import com.ruoyi.web.mapper.PhoneActiveDetailMapper;
import com.ruoyi.web.mapper.PhoneActiveInfoMapper;
import com.ruoyi.web.mapper.PhoneOrderContractMapper;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import com.ruoyi.web.service.LeaveInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private PhoneActiveDetailMapper phoneActiveDetailMapper;

    @Autowired
    private PhoneOrderContractMapper phoneOrderContractMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private LeaveInformationService leaveInformationService;

    @Autowired
    private ServerConfig serverConfig;

    /** 通信源手机的部门ID，其直接子节点即为门店 */
    private static final Long STORE_PARENT_DEPT_ID = 201L;

    @Override
    @Transactional
    public Long saveOrUpdateActiveInfo(PhoneActiveInfo info) {
        if (info == null) {
            return null;
        }
        // 更新：仅更新主表（详情和签约由各自方法处理）
        if (info.getId() != null) {
            info.setUpdateTime(new Date());
            phoneActiveInfoMapper.updateById(info);
            return info.getId();
        }
        // 新增：先插主表获取 order_id，再插详情表
        info.setImagePath(normalizeImagePath(info.getImagePath()));
        info.setCreateTime(new Date());
        info.setUpdateTime(new Date());
        phoneActiveInfoMapper.insert(info);
        Long orderId = info.getId();

        // 插入详情表（只要有任意详情字段就插入，因为详情与订单 1:1）
        PhoneActiveDetail detail = new PhoneActiveDetail();
        detail.setOrderId(orderId);
        detail.setActivated(info.getActivated());
        detail.setActivateDate(info.getActivateDate());
        detail.setCoverage(info.getCoverage());
        detail.setActiveInfo(info.getActiveInfo());
        detail.setSysTime(info.getSysTime());
        detail.setImagePath(info.getImagePath());
        phoneActiveDetailMapper.insert(detail);

        return orderId;
    }

    @Override
    @Transactional
    public void saveOrUpdateContract(PhoneOrderContract contract) {
        if (contract == null || contract.getOrderId() == null) {
            return;
        }
        PhoneOrderContract exist = phoneOrderContractMapper.selectByOrderId(contract.getOrderId());
        if (exist != null) {
            phoneOrderContractMapper.updateByOrderId(contract);
        } else {
            phoneOrderContractMapper.insert(contract);
        }
    }

    @Override
    @Transactional
    public String importActiveInfo(List<PhoneActiveInfo> list, boolean updateSupport, String operName) {
        if (list == null || list.isEmpty()) {
            return "导入数据为空";
        }

        int successCount = 0;
        int failCount = 0;
        int updateCount = 0;
        StringBuilder failMsg = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            PhoneActiveInfo info = list.get(i);
            try {
                // 1. 字段逻辑校验
                String validateError = validateImportRow(info, i + 1);
                if (validateError != null) {
                    failCount++;
                    failMsg.append("<br/>").append(validateError);
                    continue;
                }
                String sn = info.getSn();
                if (sn != null) {
                    info.setSn(sn.trim());
                }

                // 根据留资人电话匹配 infoId
                resolveLeaveInfoId(info, i + 1, failMsg);

                PhoneActiveInfo exist = info.getSn() != null ? phoneActiveInfoMapper.selectBySn(info.getSn()) : null;
                if (exist != null) {
                    if (updateSupport) {
                        info.setId(exist.getId());
                        info.setUpdateBy(operName);
                        phoneActiveInfoMapper.updateById(info);

                        // 同步更新签约表
                        PhoneOrderContract contract = new PhoneOrderContract();
                        contract.setOrderId(exist.getId());
                        contract.setSignatureModel(info.getSignatureModel());
                        contract.setSignatureImei(info.getSignatureImei());
                        contract.setSignatureDate(info.getSignatureDate());
                        saveOrUpdateContract(contract);

                        updateCount++;
                    } else {
                        failCount++;
                        failMsg.append("<br/>序列号 ").append(info.getSn()).append(" 已存在（第").append(i + 1).append("行）");
                    }
                } else {
                    info.setCreateBy(operName);
                    info.setUpdateBy(operName);
                    if (info.getStoreId() == null) {
                        info.setStoreId(resolveStoreIdByUsername(operName));
                    }
                    info.setCreateTime(new Date());
                    info.setUpdateTime(new Date());
                    phoneActiveInfoMapper.insert(info);
                    Long orderId = info.getId();

                    // 插入详情表
                    PhoneActiveDetail detail = new PhoneActiveDetail();
                    detail.setOrderId(orderId);
                    detail.setActivated(info.getActivated());
                    detail.setActivateDate(info.getActivateDate());
                    detail.setCoverage(info.getCoverage());
                    detail.setActiveInfo(info.getActiveInfo());
                    detail.setSysTime(info.getSysTime());
                    detail.setImagePath(info.getImagePath());
                    phoneActiveDetailMapper.insert(detail);

                    // 插入签约表（签名信息存在时）
                    if (hasContractData(info)) {
                        PhoneOrderContract contract = new PhoneOrderContract();
                        contract.setOrderId(orderId);
                        contract.setSignatureModel(info.getSignatureModel());
                        contract.setSignatureImei(info.getSignatureImei());
                        contract.setSignatureDate(info.getSignatureDate());
                        phoneOrderContractMapper.insert(contract);
                    }

                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                failMsg.append("<br/>第").append(i + 1).append("行导入失败：").append(e.getMessage());
            }
        }

        StringBuilder resultMsg = new StringBuilder();
        resultMsg.append("共 ").append(list.size()).append(" 条数据，成功导入 ").append(successCount).append(" 条");
        if (updateCount > 0) {
            resultMsg.append("，更新 ").append(updateCount).append(" 条");
        }
        if (failCount > 0) {
            resultMsg.append("，失败 ").append(failCount).append(" 条");
            resultMsg.append(failMsg);
        }
        return resultMsg.toString();
    }

    /** 判断是否存在签约数据 */
    private boolean hasContractData(PhoneActiveInfo info) {
        return (info.getSignatureModel() != null && !info.getSignatureModel().trim().isEmpty())
                || (info.getSignatureImei() != null && !info.getSignatureImei().trim().isEmpty())
                || (info.getSignatureDate() != null && !info.getSignatureDate().trim().isEmpty());
    }

    /**
     * 根据留资人电话匹配 leave_information 的 id
     * <p>
     * 手机号在留资表中唯一，通过精确匹配查找。
     * 如果匹配到多条（历史脏数据），取最新一条。
     * 如果匹配不到，不阻断导入，infoId 保持 null。
     */
    private void resolveLeaveInfoId(PhoneActiveInfo info, int rowNum, StringBuilder failMsg) {
        String phoneNum = info.getPhoneNum();
        if (phoneNum == null || phoneNum.trim().isEmpty()) {
            return;
        }
        phoneNum = phoneNum.trim();
        LeaveInformation query = new LeaveInformation();
        query.setPhoneNum(phoneNum);
        List<LeaveInformation> results = leaveInformationService.queryLeaveInformationByCondition(query);

        if (results == null || results.isEmpty()) {
            failMsg.append("<br/>第").append(rowNum).append("行提示：留资人电话「")
                    .append(phoneNum).append("」未在留资库中找到，请先导入留资人数据");
            return;
        }

        // 手机号唯一，取第一条；若历史存在多条脏数据取最新
        LeaveInformation match = results.get(results.size() - 1);
        info.setInfoId(match.getId());

        // 如果 Excel 未填留资人姓名，用匹配到的姓名回填
        if (info.getName() == null || info.getName().trim().isEmpty()) {
            info.setName(match.getName());
        }

        if (results.size() > 1) {
            failMsg.append("<br/>第").append(rowNum).append("行提示：留资人电话「")
                    .append(phoneNum).append("」匹配到多条记录，已取最新一条（id=")
                    .append(match.getId()).append("）");
        }
    }

    /**
     * 导入行级字段逻辑校验
     *
     * @return 错误信息，null 表示通过
     */
    private String validateImportRow(PhoneActiveInfo info, int rowNum) {
        String prefix = "第" + rowNum + "行：";

        // 创建时间必填
        if (info.getCreateTime() == null) {
            return prefix + "创建时间不能为空";
        }

        // 跳过API 必填
        if (info.getSkipApiCall() == null) {
            return prefix + "跳过API不能为空（0=旧手机识别, 1=自有渠道）";
        }
        if (info.getSkipApiCall() != 0 && info.getSkipApiCall() != 1) {
            return prefix + "跳过API值无效，必须为 0 或 1";
        }

        // 旧手机识别模式：sn 必填
        if (info.getSkipApiCall() == 0) {
            if (info.getSn() == null || info.getSn().trim().isEmpty()) {
                return prefix + "跳过API=0（旧手机识别）时，旧手机序列号不能为空";
            }
        }

        // 自有渠道模式：旧手机状态必填
        if (info.getSkipApiCall() == 1) {
            if (info.getOldPhoneStatus() == null) {
                return prefix + "跳过API=1（自有渠道）时，旧手机状态不能为空（0=无旧手机, 1=丢失/损坏）";
            }
            if (info.getOldPhoneStatus() != 0 && info.getOldPhoneStatus() != 1) {
                return prefix + "旧手机状态值无效，必须为 0 或 1";
            }
        }

        // 签约日期必填
        if (info.getSignatureDate() == null || info.getSignatureDate().trim().isEmpty()) {
            return prefix + "签约日期不能为空";
        }

        return null;
    }

    /**
     * 基于 @Excel(required=true) 注解校验必填字段
     *
     * @param info 待校验的订单对象
     * @return 错误消息，校验通过返回 null
     */
    private String validateRequiredFields(PhoneActiveInfo info) {
        Field[] fields = PhoneActiveInfo.class.getDeclaredFields();
        for (Field field : fields) {
            Excel excel = field.getAnnotation(Excel.class);
            if (excel == null || !excel.required()) {
                continue;
            }
            try {
                // 使用getter方法获取值
                Method getter = findGetter(field);
                if (getter == null) {
                    continue;
                }
                Object value = getter.invoke(info);
                if (isNullOrEmpty(value)) {
                    return excel.name() + "不能为空";
                }
            } catch (Exception e) {
                return excel.name() + "校验失败：" + e.getMessage();
            }
        }
        return null;
    }

    private boolean isNullOrEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        return false;
    }

    private Method findGetter(Field field) {
        String name = field.getName();
        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            return PhoneActiveInfo.class.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info) {
        return fillImageUrls(phoneActiveInfoMapper.selectListByExample(info));
    }

    @Override
    public List<PhoneActiveInfo> queryExportList(PhoneActiveInfo info) {
        return fillImageUrls(phoneActiveInfoMapper.selectExportList(info));
    }

    @Override
    public PhoneActiveInfo getActiveDetail(Long id) {
        if (id == null) {
            return null;
        }
        PhoneActiveInfo detail = phoneActiveInfoMapper.selectDetailById(id);
        if (detail != null) {
            fillImageUrl(detail);
        }
        return detail;
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
        long signedOrders = getLongValue(result, "signedOrders");
        long todaySignedOrders = getLongValue(result, "todaySignedOrders");
        long yesterdaySignedOrders = getLongValue(result, "yesterdaySignedOrders");

        double signedRate = calculateRatio(signedOrders, totalOrders);
        double signedGrowthRate = calculateGrowthRate(todaySignedOrders, yesterdaySignedOrders);

        Map<String, Object> statistics = new HashMap<>(20);
        statistics.put("totalOrders", totalOrders);
        statistics.put("todayOrders", todayOrders);
        statistics.put("activatedDevices", activatedDevices);
        statistics.put("pendingOrders", pendingOrders);
        statistics.put("signedOrders", signedOrders);
        statistics.put("totalOrdersGrowthRate", calculateGrowthRate(currentMonthOrders, previousMonthOrders));
        statistics.put("todayOrdersGrowthRate", calculateGrowthRate(todayOrders, yesterdayOrders));
        statistics.put("activationRate", calculateRatio(activatedDevices, totalOrders));
        statistics.put("pendingRate", calculateRatio(pendingOrders, totalOrders));
        statistics.put("signedRate", signedRate);
        statistics.put("totalOrdersGrowthLabel", buildTrendLabel(calculateGrowthRate(currentMonthOrders, previousMonthOrders), "较上月"));
        statistics.put("todayOrdersGrowthLabel", buildTrendLabel(calculateGrowthRate(todayOrders, yesterdayOrders), "较昨日"));
        statistics.put("activationRateLabel", buildRateLabel(calculateRatio(activatedDevices, totalOrders), "激活率"));
        statistics.put("pendingRateLabel", buildRateLabel(calculateRatio(pendingOrders, totalOrders), "待处理占比"));
        statistics.put("signedRateLabel", buildRateLabel(signedRate, "签约率"));
        statistics.put("totalOrdersTrend", calculateGrowthRate(currentMonthOrders, previousMonthOrders) >= 0 ? "up" : "down");
        statistics.put("todayOrdersTrend", calculateGrowthRate(todayOrders, yesterdayOrders) >= 0 ? "up" : "down");
        statistics.put("activationRateTrend", calculateRatio(activatedDevices, totalOrders) >= 0 ? "up" : "down");
        statistics.put("pendingRateTrend", calculateRatio(pendingOrders, totalOrders) <= 50 ? "down" : "up");
        statistics.put("signedRateTrend", signedGrowthRate >= 0 ? "up" : "down");
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
        long mySignedOrders = getLongValue(result, "mySignedOrders");
        long myTodaySignedOrders = getLongValue(result, "myTodaySignedOrders");
        long myYesterdaySignedOrders = getLongValue(result, "myYesterdaySignedOrders");

        double myTotalOrdersGrowthRate = calculateGrowthRate(myRecentSevenDaysOrders, myPreviousSevenDaysOrders);
        double myTodayOrdersGrowthRate = calculateGrowthRate(myTodayOrders, myYesterdayOrders);
        double myActivationRate = calculateRatio(myActivatedDevices, myTotalOrders);
        double myPendingRate = calculateRatio(myPendingOrders, myTotalOrders);
        double mySignedRate = calculateRatio(mySignedOrders, myTotalOrders);
        double mySignedGrowthRate = calculateGrowthRate(myTodaySignedOrders, myYesterdaySignedOrders);

        Map<String, Object> statistics = new HashMap<>(16);
        statistics.put("myTotalOrders", myTotalOrders);
        statistics.put("myTodayOrders", myTodayOrders);
        statistics.put("myActivatedDevices", myActivatedDevices);
        statistics.put("myPendingOrders", myPendingOrders);
        statistics.put("mySignedOrders", mySignedOrders);
        statistics.put("myTotalOrdersGrowthLabel", buildTrendLabel(myTotalOrdersGrowthRate, "近7天较前7天"));
        statistics.put("myTodayOrdersGrowthLabel", buildTrendLabel(myTodayOrdersGrowthRate, "较昨日"));
        statistics.put("myActivationRateLabel", buildRateLabel(myActivationRate, "激活率"));
        statistics.put("myPendingRateLabel", buildRateLabel(myPendingRate, "待处理占比"));
        statistics.put("mySignedRateLabel", buildRateLabel(mySignedRate, "签约率"));
        statistics.put("myTotalOrdersTrend", myTotalOrdersGrowthRate >= 0 ? "up" : "down");
        statistics.put("myTodayOrdersTrend", myTodayOrdersGrowthRate >= 0 ? "up" : "down");
        statistics.put("myActivationRateTrend", myActivationRate >= 50 ? "up" : "down");
        statistics.put("myPendingRateTrend", myPendingRate <= 50 ? "down" : "up");
        statistics.put("mySignedRateTrend", mySignedGrowthRate >= 0 ? "up" : "down");
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
        List<Long> signedOrders = new ArrayList<>();
        List<Long> myOrders = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate day = startDate.plusDays(i);
            String fullDate = day.format(DAY_FORMATTER);
            String displayDate = day.format(DateTimeFormatter.ofPattern("MM-dd"));
            Map<String, Object> row = rowMap.get(fullDate);
            long orderCount = getLongValue(row, "orderCount");
            long activatedCount = getLongValue(row, "activatedCount");
            long signedCount = getLongValue(row, "signedCount");
            dates.add(displayDate);
            newOrders.add(orderCount);
            completedOrders.add(activatedCount);
            signedOrders.add(signedCount);
            myOrders.add(orderCount);
        }

        Map<String, Object> result = new HashMap<>(8);
        result.put("dates", dates);
        result.put("newOrders", newOrders);
        result.put("completedOrders", completedOrders);
        result.put("signedOrders", signedOrders);
        result.put("myOrders", myOrders);
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeviceModelDistribution() {
        List<Map<String, Object>> result = phoneActiveInfoMapper.selectDeviceModelDistribution();
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public List<Map<String, Object>> getSignatureModelDistribution() {
        List<Map<String, Object>> result = phoneActiveInfoMapper.selectSignatureModelDistribution();
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
        List<Long> signedCounts = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            YearMonth month = startMonth.plusMonths(i);
            String key = month.format(MONTH_FORMATTER);
            Map<String, Object> row = rowMap.get(key);
            months.add(key);
            orderCounts.add(getLongValue(row, "orderCount"));
            activatedCounts.add(getLongValue(row, "activatedCount"));
            signedCounts.add(getLongValue(row, "signedCount"));
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("months", months);
        result.put("orderCounts", orderCounts);
        result.put("activatedCounts", activatedCounts);
        result.put("signedCounts", signedCounts);
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

    @Override
    public String getContractContent(Long id) {
        if (id == null) {
            return null;
        }
        return phoneActiveInfoMapper.selectContractContentById(id);
    }

    /**
     * 根据用户名解析其所属门店ID
     * 从用户的部门出发，向上查找 parent_id == 201（通信源手机）的直接子部门即为门店
     *
     * @param username 用户名
     * @return 门店ID，null表示无法解析
     */
    public Long resolveStoreIdByUsername(String username) {
        if (username == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectUserByUserName(username);
        if (user == null || user.getDeptId() == null) {
            return null;
        }
        return resolveStoreId(user.getDeptId());
    }

    /**
     * 递归向上查找门店ID
     */
    private Long resolveStoreId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = sysDeptMapper.selectDeptById(deptId);
        if (dept == null) {
            return null;
        }
        // 父部门是通信源手机 (201) → 这就是一个门店
        if (dept.getParentId() != null && dept.getParentId().equals(STORE_PARENT_DEPT_ID)) {
            return deptId;
        }
        // 本身就是通信源手机 → 不归属于某个具体门店
        if (deptId.equals(STORE_PARENT_DEPT_ID)) {
            return null;
        }
        // 继续向上找
        if (dept.getParentId() != null && dept.getParentId() != 0L) {
            return resolveStoreId(dept.getParentId());
        }
        return null;
    }
}