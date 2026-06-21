package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @ApiOperation("首页统计卡片-管理员")
    @GetMapping("/dashboard/statistics")
    public R getStatistics() {
        return R.ok(phoneActiveInfoService.getDashboardStatistics(resolveCurrentDate()));
    }

    @ApiOperation("首页订单趋势-管理员")
    @GetMapping("/dashboard/trend")
    public R getOrderTrend(@RequestParam(value = "period", defaultValue = "week") String period) {
        return R.ok(phoneActiveInfoService.getOrderTrend(period, null, resolveCurrentDate()));
    }

    @ApiOperation("首页设备型号分布-管理员")
    @GetMapping("/dashboard/deviceModelDistribution")
    public R getDeviceModelDistribution() {
        return R.ok(phoneActiveInfoService.getDeviceModelDistribution());
    }

    @ApiOperation("首页月度订单统计-管理员")
    @GetMapping("/dashboard/monthlyOrderStats")
    public R getMonthlyOrderStats() {
        return R.ok(phoneActiveInfoService.getMonthlyOrderStats(resolveCurrentDate()));
    }

    @ApiOperation("首页保修状态统计-管理员")
    @GetMapping("/dashboard/warrantyStatus")
    public R getWarrantyStatus() {
        return R.ok(phoneActiveInfoService.getWarrantyStatus(resolveCurrentDate()));
    }

    @ApiOperation("首页最近订单-管理员")
    @GetMapping("/dashboard/recentOrders")
    public R getRecentOrders() {
        return R.ok(phoneActiveInfoService.getRecentOrders(null, DASHBOARD_RECENT_LIMIT));
    }

    @ApiOperation("首页个人统计卡片")
    @GetMapping("/dashboard/userStatistics")
    public R getUserStatistics() {
        return R.ok(phoneActiveInfoService.getUserDashboardStatistics(getUsername(), resolveCurrentDate()));
    }

    @ApiOperation("首页个人订单趋势")
    @GetMapping("/dashboard/userTrend")
    public R getUserOrderTrend() {
        return R.ok(phoneActiveInfoService.getOrderTrend("week", getUsername(), resolveCurrentDate()));
    }

    @ApiOperation("首页个人最近订单")
    @GetMapping("/dashboard/userRecentOrders")
    public R getUserRecentOrders() {
        return R.ok(phoneActiveInfoService.getRecentOrders(getUsername(), DASHBOARD_RECENT_LIMIT));
    }

    private String resolveCurrentDate() {
        // 仪表盘首页始终使用服务器真实日期，不受 sys.time 业务配置影响
        // sys.time 配置仅用于手机设备系统时间的同步，不应影响后台管理界面的数据查询
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * @param phoneActiveInfo 查询条件
     * @return 订单分页列表
     */
    @ApiOperation("查询已签约订单列表")
    @GetMapping("/querySignContractOrderList")
    public R querySignContractOrderList(PhoneActiveInfo phoneActiveInfo) {
        startPage();
        phoneActiveInfo.setIsSignature(1);
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryActiveList(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }
}
