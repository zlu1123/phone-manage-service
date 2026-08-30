package com.ruoyi.web.service.impl;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.web.domain.PhoneActiveDetail;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.domain.PhoneActiveInfoImport;
import com.ruoyi.web.domain.PhoneOrderContract;
import com.ruoyi.web.domain.LeaveInformation;
import com.ruoyi.web.mapper.LeaveInformationMapper;
import com.ruoyi.web.mapper.PhoneActiveDetailMapper;
import com.ruoyi.web.mapper.PhoneActiveInfoMapper;
import com.ruoyi.web.mapper.PhoneOrderContractMapper;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private LeaveInformationMapper leaveInformationMapper;

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
    public Map<String, Object> importActiveInfo(List<PhoneActiveInfoImport> list, boolean updateSupport, String operName) {
        Map<String, Object> result = new HashMap<>(8);
        result.put("total", list == null ? 0 : list.size());
        List<String> errors = new ArrayList<>();
        List<String> warns = new ArrayList<>();
        result.put("errors", errors);
        result.put("warns", warns);

        if (list == null || list.isEmpty()) {
            result.put("successCount", 0);
            result.put("updateCount", 0);
            result.put("failCount", 0);
            result.put("message", "导入数据为空");
            return result;
        }

        // 第一阶段：对全部行做字段校验（含门店匹配、文件内序列号去重），
        // 任何一行校验不通过则整个文件不导入，不会写入任何数据。
        List<ParsedImportRow> parsedRows = new ArrayList<>();
        Set<String> processedSns = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            PhoneActiveInfoImport row = list.get(i);
            int rowNum = i + 1;
            try {
                if (row == null) {
                    errors.add("第" + rowNum + "行：未解析到有效数据");
                    continue;
                }
                String validateError = validateImportRow(row, rowNum);
                if (validateError != null) {
                    errors.add(validateError);
                    continue;
                }
                PhoneActiveInfo info = buildActiveInfo(row, operName);
                String sn = info.getSn();
                if (sn != null) {
                    if (processedSns.contains(sn)) {
                        errors.add("第" + rowNum + "行：序列号「" + sn + "」与文件内其他行重复");
                        continue;
                    }
                    processedSns.add(sn);
                }
                parsedRows.add(new ParsedImportRow(rowNum, row, info));
            } catch (IllegalArgumentException e) {
                // 门店匹配失败等校验类异常：该行不通过，整体取消导入
                errors.add("第" + rowNum + "行：" + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            result.put("successCount", 0);
            result.put("updateCount", 0);
            result.put("failCount", errors.size());
            result.put("message", buildImportResultMessage(list.size(), 0, 0, errors.size(), errors, warns)
                    + "<br/>文件校验未通过，已取消导入，未写入任何数据");
            return result;
        }

        // 第二阶段：全部校验通过后逐行入库，运行期冲突（如序列号已存在）仅影响对应行。
        int successCount = 0;
        int updateCount = 0;
        int failCount = 0;
        for (ParsedImportRow parsed : parsedRows) {
            int rowNum = parsed.rowNum;
            PhoneActiveInfoImport row = parsed.row;
            PhoneActiveInfo info = parsed.info;
            try {
                // 根据留资人电话匹配留资记录，匹配不到时自动创建（入库阶段执行，避免校验不通过时产生脏数据）
                resolveLeaveInfo(info, row, operName, rowNum, warns);
                // 根据 sn 判断新增或更新
                PhoneActiveInfo exist = info.getSn() != null ? phoneActiveInfoMapper.selectBySn(info.getSn()) : null;
                if (exist != null) {
                    if (!updateSupport) {
                        errors.add("第" + rowNum + "行：序列号「" + info.getSn() + "」已存在");
                        failCount++;
                    } else {
                        updateExistingOrder(info, row, exist, operName);
                        updateCount++;
                    }
                } else {
                    insertNewOrder(info, row, operName);
                    successCount++;
                }
            } catch (DuplicateKeyException e) {
                errors.add("第" + rowNum + "行：序列号已存在或数据冲突，导入失败");
                failCount++;
            } catch (Exception e) {
                errors.add("第" + rowNum + "行导入失败："
                        + (StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName()));
                failCount++;
            }
        }

        result.put("successCount", successCount);
        result.put("updateCount", updateCount);
        result.put("failCount", failCount);
        result.put("message", buildImportResultMessage(list.size(), successCount, updateCount, failCount, errors, warns));
        return result;
    }

    @Override
    public int markTestDataByIds(Long[] ids, String operName) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return phoneActiveInfoMapper.updateDelFlagByIds(ids, operName);
    }

    /** 校验通过后待入库的行数据 */
    private static class ParsedImportRow {
        final int rowNum;
        final PhoneActiveInfoImport row;
        final PhoneActiveInfo info;

        ParsedImportRow(int rowNum, PhoneActiveInfoImport row, PhoneActiveInfo info) {
            this.rowNum = rowNum;
            this.row = row;
            this.info = info;
        }
    }

    /**
     * 导入行级字段逻辑校验
     * <p>
     * 导入模型统一用 String 接收数值/枚举列，此处负责值域校验，
     * 保证后续解析（{@link #buildActiveInfo}）不会因格式问题抛异常。
     *
     * @return 错误信息，null 表示通过
     */
    private String validateImportRow(PhoneActiveInfoImport row, int rowNum) {
        String prefix = "第" + rowNum + "行：";

        // 创建时间必填
        if (row.getCreateTime() == null) {
            return prefix + "创建时间不能为空";
        }

        // 跳过API 必填 + 值域（兼容旧模板的 0/1，新模板填 自有/亚丁）
        if (trimToNull(row.getSkipApiCall()) == null) {
            return prefix + "跳过API不能为空（自有/亚丁，或旧模板的 0/1）";
        }
        if (parseSkipApiCall(row.getSkipApiCall()) == null) {
            return prefix + "跳过API值无效，必须为 0/1 或 自有/亚丁";
        }

        // 旧手机状态：选填（照片已不存在，新数据不提供），填了必须在值域内
        String oldPhoneStatus = trimToNull(row.getOldPhoneStatus());
        if (oldPhoneStatus != null && !"0".equals(oldPhoneStatus) && !"1".equals(oldPhoneStatus)) {
            return prefix + "旧手机状态值无效，必须为 0 或 1";
        }

        // 旧手机使用月数：选填，填了必须为正整数
        String usageMonths = trimToNull(row.getOldPhoneUsageMonths());
        if (usageMonths != null && !usageMonths.matches("\\d+(\\.0+)?")) {
            return prefix + "旧手机使用月数无效，必须为正整数";
        }

        // 鸭宝激活状态：选填，填了必须在枚举范围内
        String activated = trimToNull(row.getActivated());
        if (activated != null && parseActivated(activated) == null) {
            return prefix + "鸭宝激活状态值无效，可填写：已激活/未激活（或 true/false、1/0、是/否）";
        }

        return null;
    }

    /**
     * 解析「跳过API」列：兼容旧模板的 0/1，也支持新模板直接填渠道文本 自有/亚丁
     * <ul>
     *     <li>自有 → 1（自有渠道，跳过API查询）</li>
     *     <li>亚丁 → 0（走API识别的亚丁渠道）</li>
     * </ul>
     *
     * @return 解析后的 skipApiCall 值，无法识别返回 null
     */
    private Integer parseSkipApiCall(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        if ("0".equals(v) || "亚丁".equals(v)) {
            return 0;
        }
        if ("1".equals(v) || "自有".equals(v)) {
            return 1;
        }
        return null;
    }

    /** 由 skipApiCall 值还原渠道文本（兼容旧模板：0→亚丁，1→自有） */
    private String channelOf(Integer skipApiCall) {
        if (skipApiCall == null) {
            return null;
        }
        return skipApiCall == 0 ? "亚丁" : "自有";
    }

    /** 将导入模型解析为订单对象（含门店归属解析，留资匹配另行处理） */
    private PhoneActiveInfo buildActiveInfo(PhoneActiveInfoImport row, String operName) {
        PhoneActiveInfo info = new PhoneActiveInfo();
        info.setSn(trimToNull(row.getSn()));
        info.setPhoneType(trimToNull(row.getPhoneType()));
        info.setModel(trimToNull(row.getModel()));
        info.setImei1(trimToNull(row.getImei1()));
        info.setImei2(trimToNull(row.getImei2()));
        info.setNickName(trimToNull(row.getNickName()));
        Integer skipApiCall = parseSkipApiCall(row.getSkipApiCall());
        info.setSkipApiCall(skipApiCall);
        info.setChannel(channelOf(skipApiCall));
        // 旧照片不存在，新数据无旧手机信息：自有渠道统一按「无旧手机」落库，亚丁渠道不记录旧手机状态
        Integer oldPhoneStatus = parseIntOrNull(row.getOldPhoneStatus());
        info.setOldPhoneStatus(oldPhoneStatus != null ? oldPhoneStatus : (skipApiCall != null && skipApiCall == 1 ? 0 : null));
        info.setOldPhoneUsageMonths(parseUsageMonthsOrNull(row.getOldPhoneUsageMonths()));
        info.setCreateTime(row.getCreateTime());
        info.setStoreId(resolveImportStoreId(row.getStore(), operName));
        info.setSignatureModel(trimToNull(row.getSignatureModel()));
        info.setSignatureImei(trimToNull(row.getSignatureImei()));
        // 没有签约时间：以创建时间作为签约日期
        info.setSignatureDate(resolveSignatureDate(row));
        return info;
    }

    /** 签约日期：填了用填的值，没填默认取创建时间（yyyy-MM-dd） */
    private String resolveSignatureDate(PhoneActiveInfoImport row) {
        String signatureDate = trimToNull(row.getSignatureDate());
        if (signatureDate != null || row.getCreateTime() == null) {
            return signatureDate;
        }
        return row.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .format(DAY_FORMATTER);
    }

    /**
     * 解析导入的所属门店列：可填门店名称（系统自动匹配）或门店ID，留空则归属当前操作人所属门店
     * <p>
     * 填写了但匹配不到时抛出异常（该行导入失败），避免订单被静默归属到错误门店。
     */
    private Long resolveImportStoreId(String store, String operName) {
        String value = trimToNull(store);
        if (value == null) {
            return resolveStoreIdByUsername(operName);
        }
        Long storeId = value.matches("\\d+")
                ? resolveStoreIdById(Long.valueOf(value))
                : phoneActiveInfoMapper.selectStoreIdByName(value, STORE_PARENT_DEPT_ID);
        if (storeId == null) {
            throw new IllegalArgumentException("所属门店「" + value + "」未匹配到，请填写正确的门店名称或门店ID");
        }
        return storeId;
    }

    /** 校验数字门店ID是否为通信源手机下的有效门店（启用且未删除） */
    private Long resolveStoreIdById(Long storeId) {
        if (storeId == null) {
            return null;
        }
        SysDept dept = sysDeptMapper.selectDeptById(storeId);
        if (dept == null || !"0".equals(dept.getStatus()) || !"0".equals(dept.getDelFlag())) {
            return null;
        }
        return dept.getParentId() != null && dept.getParentId().equals(STORE_PARENT_DEPT_ID) ? storeId : null;
    }

    /**
     * 根据留资人电话匹配留资记录，匹配不到时自动创建留资记录（需要留资人姓名）
     */
    private void resolveLeaveInfo(PhoneActiveInfo info, PhoneActiveInfoImport row, String operName,
                                  int rowNum, List<String> warns) {
        String phoneNum = trimToNull(row.getPhoneNum());
        if (phoneNum == null) {
            return;
        }
        LeaveInformation exist = leaveInformationMapper.selectByPhoneNum(phoneNum);
        if (exist != null) {
            info.setInfoId(exist.getId());
            return;
        }
        // 匹配不到且缺留资人姓名时无法自动创建，仅提示不阻断导入
        String name = trimToNull(row.getName());
        if (name == null) {
            warns.add("第" + rowNum + "行提示：留资人电话「" + phoneNum + "」未在留资库中匹配到，且未填写留资人姓名，未自动创建留资记录");
            return;
        }
        LeaveInformation created = new LeaveInformation();
        created.setName(name);
        created.setPhoneNum(phoneNum);
        created.setCreateBy(operName);
        created.setUpdateBy(operName);
        created.setCreateTime(new Date());
        created.setUpdateTime(new Date());
        leaveInformationMapper.insert(created);
        info.setInfoId(created.getId());
        warns.add("第" + rowNum + "行提示：留资人电话「" + phoneNum + "」未在留资库中匹配到，已自动创建留资记录（" + name + "）");
    }

    /** 更新已存在的订单：主表/详情/签约均只更新非空字段，避免覆盖未填写的列 */
    private void updateExistingOrder(PhoneActiveInfo info, PhoneActiveInfoImport row,
                                     PhoneActiveInfo exist, String operName) {
        info.setId(exist.getId());
        info.setUpdateBy(operName);
        // 门店未填写时不覆盖原门店快照
        if (trimToNull(row.getStore()) == null) {
            info.setStoreId(null);
        }
        phoneActiveInfoMapper.updateById(info);

        PhoneActiveDetail detail = buildDetail(exist.getId(), row);
        if (hasDetailData(detail)) {
            phoneActiveDetailMapper.updateByOrderIdSelective(detail);
        }
        if (hasContractData(row)) {
            PhoneOrderContract contract = buildContract(exist.getId(), row);
            if (phoneOrderContractMapper.selectByOrderId(exist.getId()) != null) {
                phoneOrderContractMapper.updateByOrderIdSelective(contract);
            } else {
                phoneOrderContractMapper.insert(contract);
            }
        }
    }

    /** 新增订单：主表/详情/签约三表落库 */
    private void insertNewOrder(PhoneActiveInfo info, PhoneActiveInfoImport row, String operName) {
        info.setCreateBy(operName);
        info.setUpdateBy(operName);
        if (info.getCreateTime() == null) {
            info.setCreateTime(new Date());
        }
        info.setUpdateTime(new Date());
        phoneActiveInfoMapper.insert(info);
        Long orderId = info.getId();

        phoneActiveDetailMapper.insert(buildDetail(orderId, row));
        if (hasContractData(row)) {
            phoneOrderContractMapper.insert(buildContract(orderId, row));
        }
    }

    /** 由导入行构建详情表数据 */
    private PhoneActiveDetail buildDetail(Long orderId, PhoneActiveInfoImport row) {
        PhoneActiveDetail detail = new PhoneActiveDetail();
        detail.setOrderId(orderId);
        detail.setActivated(parseActivated(trimToNull(row.getActivated())));
        detail.setActivateDate(trimToNull(row.getActivateDate()));
        detail.setCoverage(trimToNull(row.getCoverage()));
        detail.setSysTime(trimToNull(row.getSysTime()));
        return detail;
    }

    /** 详情表是否存在非空字段（为空时跳过 selective 更新，避免生成空 SET 语句） */
    private boolean hasDetailData(PhoneActiveDetail detail) {
        return detail.getActivated() != null
                || detail.getActivateDate() != null
                || detail.getCoverage() != null
                || detail.getSysTime() != null;
    }

    /** 由导入行构建签约表数据（签约日期未填时默认取创建时间） */
    private PhoneOrderContract buildContract(Long orderId, PhoneActiveInfoImport row) {
        PhoneOrderContract contract = new PhoneOrderContract();
        contract.setOrderId(orderId);
        contract.setSignatureModel(trimToNull(row.getSignatureModel()));
        contract.setSignatureImei(trimToNull(row.getSignatureImei()));
        contract.setSignatureDate(resolveSignatureDate(row));
        return contract;
    }

    /** 判断导入行是否存在签约数据（签约日期默认为创建时间，不作为判断依据） */
    private boolean hasContractData(PhoneActiveInfoImport row) {
        return trimToNull(row.getSignatureModel()) != null
                || trimToNull(row.getSignatureImei()) != null
                || trimToNull(row.getSignatureDate()) != null;
    }

    /**
     * 解析鸭宝激活状态：已激活/未激活、true/false、1/0、是/否，无法识别返回 null
     */
    private Boolean parseActivated(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        if ("已激活".equals(v) || "是".equals(v) || "1".equals(v) || "true".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        }
        if ("未激活".equals(v) || "否".equals(v) || "0".equals(v) || "false".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Integer parseIntOrNull(String value) {
        String v = trimToNull(value);
        return v == null ? null : Integer.valueOf(v);
    }

    /** 旧手机使用月数：兼容 Excel 数值列产生的 "24.0" 形式 */
    private Integer parseUsageMonthsOrNull(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        return new BigDecimal(v).intValue();
    }

    /** 组装导入结果摘要（展示给用户，包含错误与提示明细） */
    private String buildImportResultMessage(int total, int successCount, int updateCount, int failCount,
                                            List<String> errors, List<String> warns) {
        StringBuilder message = new StringBuilder();
        message.append("共 ").append(total).append(" 条数据，成功导入 ").append(successCount).append(" 条");
        if (updateCount > 0) {
            message.append("，更新 ").append(updateCount).append(" 条");
        }
        if (failCount > 0) {
            message.append("，失败 ").append(failCount).append(" 条");
        }
        if (!warns.isEmpty()) {
            message.append("<br/>").append(String.join("<br/>", warns));
        }
        if (!errors.isEmpty()) {
            message.append("<br/>").append(String.join("<br/>", errors));
        }
        return message.toString();
    }

    /** 去除首尾空白（含 Excel 常见的不间断空格、全角空格），空串返回 null */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replaceAll("^[\\s\\u00A0\\u3000]+|[\\s\\u00A0\\u3000]+$", "");
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public List<PhoneActiveInfo> queryActiveList(PhoneActiveInfo info) {
        List<PhoneActiveInfo> list = phoneActiveInfoMapper.selectListByExample(info);
        list.forEach(this::normalizeDates);
        return fillImageUrls(list);
    }

    @Override
    public List<PhoneActiveInfo> queryExportList(PhoneActiveInfo info) {
        List<PhoneActiveInfo> list = phoneActiveInfoMapper.selectExportList(info);
        list.forEach(this::normalizeDates);
        return fillImageUrls(list);
    }

    @Override
    public PhoneActiveInfo getActiveDetail(Long id) {
        if (id == null) {
            return null;
        }
        PhoneActiveInfo detail = phoneActiveInfoMapper.selectDetailById(id);
        if (detail != null) {
            normalizeDates(detail);
            fillImageUrl(detail);
        }
        return detail;
    }

    @Override
    public Map<String, Object> getDashboardStatistics(String currentDate, Long storeId) {
        LocalDate businessDate = parseDate(currentDate);
        String previousDate = businessDate.minusDays(1).format(DAY_FORMATTER);
        String currentMonth = businessDate.format(MONTH_FORMATTER);
        String previousMonth = businessDate.minusMonths(1).format(MONTH_FORMATTER);

        Map<String, Object> result = phoneActiveInfoMapper.selectDashboardStatistics(
                businessDate.format(DAY_FORMATTER), previousDate, currentMonth, previousMonth, storeId);
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
    public Map<String, Object> getOrderTrend(String period, String createBy, String currentDate, Long storeId) {
        int days = "month".equalsIgnoreCase(period) ? 30 : 7;
        LocalDate endDate = parseDate(currentDate);
        LocalDate startDate = endDate.minusDays(days - 1L);
        List<Map<String, Object>> rows = phoneActiveInfoMapper.selectOrderTrend(
                startDate.format(DAY_FORMATTER),
                endDate.format(DAY_FORMATTER),
                createBy,
                storeId);

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
    public Map<String, Object> getMonthlyOrderStats(String currentDate, Long storeId) {
        LocalDate businessDate = parseDate(currentDate);
        YearMonth currentMonth = YearMonth.from(businessDate);
        YearMonth startMonth = currentMonth.minusMonths(5);
        List<Map<String, Object>> rows = phoneActiveInfoMapper.selectMonthlyOrderStats(
                startMonth.format(MONTH_FORMATTER),
                currentMonth.format(MONTH_FORMATTER),
                storeId);

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
    public List<PhoneActiveInfo> getRecentOrders(String createBy, int limit, Long storeId) {
        List<PhoneActiveInfo> list = phoneActiveInfoMapper.selectRecentOrders(createBy, limit, storeId);
        list.forEach(this::normalizeDates);
        return fillImageUrls(list);
    }

    @Override
    public List<Map<String, Object>> getStoreList() {
        List<Map<String, Object>> list = phoneActiveInfoMapper.selectStoreList(STORE_PARENT_DEPT_ID);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<Map<String, Object>> getStoreComparison() {
        List<Map<String, Object>> list = phoneActiveInfoMapper.selectStoreComparison(STORE_PARENT_DEPT_ID);
        return list == null ? Collections.emptyList() : list;
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

    /**
     * 统一日期格式为 yyyy-MM-dd，避免各品牌API返回的中文日期（如"2025年10月17日"）导致编码乱码
     */
    private void normalizeDates(PhoneActiveInfo info) {
        if (info == null) {
            return;
        }
        info.setActivateDate(normalizeDateStr(info.getActivateDate()));
        info.setCoverage(normalizeDateStr(info.getCoverage()));
    }

    private String normalizeDateStr(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return dateStr;
        }
        String s = dateStr.trim();
        // 中文格式：2025年10月17日 → 2025-10-17
        java.util.regex.Matcher cnMatcher = java.util.regex.Pattern.compile(
                "(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日").matcher(s);
        if (cnMatcher.find()) {
            return String.format("%04d-%02d-%02d",
                    Integer.parseInt(cnMatcher.group(1)),
                    Integer.parseInt(cnMatcher.group(2)),
                    Integer.parseInt(cnMatcher.group(3)));
        }
        // 斜杠格式：2025/10/17 → 2025-10-17
        java.util.regex.Matcher slashMatcher = java.util.regex.Pattern.compile(
                "(\\d{4})\\s*/\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})").matcher(s);
        if (slashMatcher.find()) {
            return String.format("%04d-%02d-%02d",
                    Integer.parseInt(slashMatcher.group(1)),
                    Integer.parseInt(slashMatcher.group(2)),
                    Integer.parseInt(slashMatcher.group(3)));
        }
        return s;
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