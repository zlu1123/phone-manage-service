package com.ruoyi.web.controller.tool;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.service.impl.ExternalApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("06api")
@Slf4j
@RestController
@RequestMapping("/06/api")
public class Api06Controller extends BaseController {

    @Autowired
    private ExternalApiService externalApiService;

    @ApiOperation("查询余额")
    @GetMapping("/balance")
    public R balance() {
        ExternalApiService.ApiResult result = externalApiService.getBalance();
        return R.ok(result.getData());
    }

    @ApiOperation("查询手机型号列表")
    @GetMapping("/queryPhoneTypeList")
    public R queryPhoneTypeList() {
        return R.ok(PhoneType.toMapList());
    }
}