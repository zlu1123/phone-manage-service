package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ruoyi.common.utils.PageUtils.startPage;
import static com.ruoyi.common.utils.SecurityUtils.getUsername;


@Api("后台订单管理")
@Slf4j
@RestController
@RequestMapping("/system/order/")
public class PhoneActiveOrderController extends BaseController {

    @Autowired
    private IPhoneActiveInfoService phoneActiveInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @param type 手机类型编号
     * @param code sn码
     * @return
     */
    @ApiOperation("查询订单列表")
    @GetMapping("/queryOrderList")
    public R queryOrderList(PhoneActiveInfo phoneActiveInfo) {
        startPage();
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryActiveList(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }

}
