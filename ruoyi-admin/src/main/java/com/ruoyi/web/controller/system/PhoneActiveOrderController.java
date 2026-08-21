package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.mapper.PhoneActiveInfoMapper;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.ruoyi.common.utils.PageUtils.startPage;
import static com.ruoyi.common.utils.SecurityUtils.getUsername;

@Api("后台订单管理")
@Slf4j
@RestController
@RequestMapping("/system/order")
public class PhoneActiveOrderController extends BaseController {

    private static final int DASHBOARD_RECENT_LIMIT = 8;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private IPhoneActiveInfoService phoneActiveInfoService;

    @Autowired
    private PhoneActiveInfoMapper phoneActiveInfoMapper;

    /**
     * @param phoneActiveInfo 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询订单列表")
    @GetMapping("/queryOrderList")
    public R queryOrderList(PhoneActiveInfo phoneActiveInfo) {
        startPage();
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryActiveList(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }

    /**
     * 导出订单数据（全字段，不分页）
     *
     * @param phoneActiveInfo 筛选条件
     * @return 订单全量数据（不含分页包装）
     */
    @ApiOperation("导出订单数据")
    @GetMapping("/exportOrderList")
    public R exportOrderList(PhoneActiveInfo phoneActiveInfo) {
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryExportList(phoneActiveInfo);
        return R.ok(list);
    }

    @ApiOperation("首页统计卡片-管理员")
    @GetMapping("/dashboard/statistics")
    public R getStatistics(@RequestParam(value = "storeId", required = false) Long storeId) {
        return R.ok(phoneActiveInfoService.getDashboardStatistics(resolveCurrentDate(), storeId));
    }

    @ApiOperation("首页订单趋势-管理员")
    @GetMapping("/dashboard/trend")
    public R getOrderTrend(@RequestParam(value = "period", defaultValue = "week") String period,
                           @RequestParam(value = "storeId", required = false) Long storeId) {
        return R.ok(phoneActiveInfoService.getOrderTrend(period, null, resolveCurrentDate(), storeId));
    }

    @ApiOperation("首页设备型号分布-管理员")
    @GetMapping("/dashboard/deviceModelDistribution")
    public R getDeviceModelDistribution() {
        return R.ok(phoneActiveInfoService.getDeviceModelDistribution());
    }

    @ApiOperation("首页已签约手机型号分布-管理员")
    @GetMapping("/dashboard/signatureModelDistribution")
    public R getSignatureModelDistribution() {
        return R.ok(phoneActiveInfoService.getSignatureModelDistribution());
    }

    @ApiOperation("首页月度订单统计-管理员")
    @GetMapping("/dashboard/monthlyOrderStats")
    public R getMonthlyOrderStats(@RequestParam(value = "storeId", required = false) Long storeId) {
        return R.ok(phoneActiveInfoService.getMonthlyOrderStats(resolveCurrentDate(), storeId));
    }

    @ApiOperation("首页保修状态统计-管理员")
    @GetMapping("/dashboard/warrantyStatus")
    public R getWarrantyStatus() {
        return R.ok(phoneActiveInfoService.getWarrantyStatus(resolveCurrentDate()));
    }

    @ApiOperation("首页最近订单-管理员")
    @GetMapping("/dashboard/recentOrders")
    public R getRecentOrders(@RequestParam(value = "storeId", required = false) Long storeId) {
        return R.ok(phoneActiveInfoService.getRecentOrders(null, DASHBOARD_RECENT_LIMIT, storeId));
    }

    @ApiOperation("首页门店列表")
    @GetMapping("/dashboard/storeList")
    public R getStoreList() {
        return R.ok(phoneActiveInfoService.getStoreList());
    }

    @ApiOperation("首页门店对比统计")
    @GetMapping("/dashboard/storeComparison")
    public R getStoreComparison() {
        return R.ok(phoneActiveInfoService.getStoreComparison());
    }

    @ApiOperation("首页个人统计卡片")
    @GetMapping("/dashboard/userStatistics")
    public R getUserStatistics() {
        return R.ok(phoneActiveInfoService.getUserDashboardStatistics(getUsername(), resolveCurrentDate()));
    }

    @ApiOperation("首页个人订单趋势")
    @GetMapping("/dashboard/userTrend")
    public R getUserOrderTrend() {
        return R.ok(phoneActiveInfoService.getOrderTrend("week", getUsername(), resolveCurrentDate(), null));
    }

    @ApiOperation("首页个人最近订单")
    @GetMapping("/dashboard/userRecentOrders")
    public R getUserRecentOrders() {
        return R.ok(phoneActiveInfoService.getRecentOrders(getUsername(), DASHBOARD_RECENT_LIMIT, null));
    }

    private String resolveCurrentDate() {
        // 仪表盘首页始终使用服务器真实日期，不受 sys.time 业务配置影响
        // sys.time 配置仅用于手机设备系统时间的同步，不应影响后台管理界面的数据查询
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * @param phoneActiveInfo 查询条件
     * @return 已签约订单分页列表
     */
    @ApiOperation("查询已签约订单列表")
    @GetMapping("/querySignContractOrderList")
    public R querySignContractOrderList(PhoneActiveInfo phoneActiveInfo) {
        startPage();
        phoneActiveInfo.setIsSignature(1);
        List<PhoneActiveInfo> list = phoneActiveInfoMapper.selectByExample(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }

    /**
     * 获取订单详情（全字段，包含详情表、签约表等所有关联数据）
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @ApiOperation("获取订单详情")
    @GetMapping("/getDetail")
    public R getOrderDetail(@RequestParam("id") Long id) {
        PhoneActiveInfo detail = phoneActiveInfoService.getActiveDetail(id);
        if (detail == null) {
            return R.fail("订单不存在");
        }
        return R.ok(detail);
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载订单导入模板")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<PhoneActiveInfo> util = new ExcelUtil<>(PhoneActiveInfo.class);
        util.importTemplateExcel(response, "订单数据");
    }

    /**
     * 导入订单数据
     *
     * @param file 上传的Excel文件
     * @param updateSupport 是否更新已存在的数据
     */
    @ApiOperation("导入订单数据")
    @PostMapping("/importData")
    public R importData(MultipartFile file, @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception {
        ExcelUtil<PhoneActiveInfo> util = new ExcelUtil<>(PhoneActiveInfo.class);
        List<PhoneActiveInfo> list = util.importExcel(file.getInputStream());
        String message = phoneActiveInfoService.importActiveInfo(list, updateSupport, getUsername());
        return R.ok(message);
    }

    /**
     * 获取订单签约时的协议内容（历史快照，不受协议模板表变更影响）
     *
     * @param id 订单ID
     * @return 协议内容（富文本HTML）
     */
    @ApiOperation("获取订单签约协议内容")
    @GetMapping("/getContractContent")
    public R getContractContent(@RequestParam("id") Long id) {
        String content = phoneActiveInfoService.getContractContent(id);
        if (content == null || content.trim().isEmpty()) {
            return R.fail("该订单暂无协议内容");
        }
        return R.ok(content);
    }
}
