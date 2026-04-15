package com.ruoyi.web.controller.wechat;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.web.core.config.PhoneInfoConverterContext;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import com.ruoyi.web.service.MapToObjectConverter;
import com.ruoyi.web.service.PhoneInfoConverter;
import com.ruoyi.web.service.impl.ExternalApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


@Api("小程序接口")
@Slf4j
@RestController
@RequestMapping("/wechat/api/")
public class WechatApiController extends BaseController {

    @Autowired
    private ExternalApiService externalApiService;
    @Autowired
    private PhoneInfoConverterContext converterContext;
    @Autowired
    private IPhoneActiveInfoService phoneActiveInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 查询激活信息
     * 查询策略：先用SN码查询，如果失败再用IMEI查询，两次都失败返回匹配失败
     *
     * @param typeCode 手机类型编号
     * @param code     SN码
     * @param imei     IMEI码（可选，用于SN查询失败时的重试）
     * @return 激活信息
     */
    @ApiOperation("查询激活信息")
    @GetMapping("/queryActiveInfo")
    public R queryActiveInfo(@RequestParam("typeCode") String typeCode,
                             @RequestParam("code") String code,
                             @RequestParam(value = "imei", required = false) String imei) {
        String type = PhoneType.getValueByCode(typeCode);
        if (type == null) {
            return R.fail("查询失败，无效的手机类型！");
        }

        // 第一次：用SN码查询
        log.info("第一次查询：使用SN码={}, type={}", code, type);
        ExternalApiService.ApiResult apiResult = externalApiService.fetchDataFromExternalApi(type, code);

        // 第二次：SN查询失败且有IMEI时，用IMEI重试
        if (!apiResult.getSuccess() && imei != null && !imei.trim().isEmpty()) {
            log.info("SN查询失败，第二次查询：使用IMEI={}, type={}", imei, type);
            apiResult = externalApiService.fetchDataFromExternalApi(type, imei.trim());
        }

        // 两次都失败，返回匹配失败
        if (!apiResult.getSuccess()) {
            return R.fail("查询失败，请检查输入是否正确！" + apiResult.getData());
        }

        // 存库，封装（需要的信息返回，其他原始数据以json形式存库）
        PhoneInfoDto phoneInfoDto = handleObject(typeCode, apiResult.getData());
        if (phoneInfoDto == null) {
            return R.fail("数据解析失败");
        }
        try {
            phoneInfoDto.setSysTime((String) redisTemplate.opsForValue().get(CacheConstants.SYS_CONFIG_KEY + Constants.SYSTEM_TIME_CACHE_KEY));
            saveActiveInfo(phoneInfoDto, apiResult.getRawJson(), typeCode);
        } catch (Exception e) {
            log.error("保存数据失败", e);
        }
        return R.ok(phoneInfoDto);
    }

    /**
     * 处理不同厂商手机类型数据
     * @param type 手机类型
     * @param object 激活信息
     * @return
     */
    private PhoneInfoDto handleObject(String type, Object object) {
        PhoneInfoDto dto = new PhoneInfoDto();
        if (object == null) {
            return dto;
        }
        // 1. 将 Map 转换为强类型对象
        MapToObjectConverter objectConverter = converterContext.getObjectConverter(type);
        if (objectConverter == null) {
            // 未找到对应策略，可记录日志或抛出异常
            log.warn("No object converter found for type: {}", type);
            return dto;
        }
        Object typedObject = objectConverter.convert((Map<String, Object>) object);

        // 2. 使用原有策略填充 DTO
        PhoneInfoConverter converter = converterContext.getConverter(type);
        if (converter != null) {
            converter.convert(typedObject, dto);
        } else {
            // 未找到对应策略，可记录日志或抛出异常
            log.warn("No converter found for type: {}", type);
        }
        return dto;
    }

    private void saveActiveInfo(PhoneInfoDto dto, String rawJson, String typeCode) {
        log.info("开始保存信息，{}", dto);
        PhoneActiveInfo info = new PhoneActiveInfo();
        info.setSn(dto.getSn());
        info.setPhoneType(typeCode);
        info.setImei1(dto.getImei1());
        info.setImei2(dto.getImei2());
        info.setModel(dto.getModel());
        info.setActivated(dto.getActivated());
        info.setActivateDate(dto.getActivateDate());
        info.setCoverage(dto.getCoverage());
        info.setActiveInfo(rawJson); // 原始完整 JSON
        info.setSysTime(dto.getSysTime());
        // 设置创建人/更新人
        info.setCreateBy(getUsername());
        info.setUpdateBy(getUsername());
        phoneActiveInfoService.saveOrUpdateActiveInfo(info);
    }

    @ApiOperation("查询订单列表-个人")
    @GetMapping("/queryOrderList")
    public R queryOrderList() {
        startPage();
        String userName = getUsername();
        PhoneActiveInfo phoneActiveInfo = new PhoneActiveInfo();
        phoneActiveInfo.setCreateBy(userName);
        List<PhoneActiveInfo> list = phoneActiveInfoService.queryActiveList(phoneActiveInfo);
        return R.ok(getDataTable(list));
    }

    @Autowired
    private ISysConfigService configService;
    @ApiOperation("查询订单列表-个人")
    @GetMapping("/test")
    public R test(){
        System.out.println("定时任务--------系统时间更新开始");
        String systemDate = (String)redisTemplate.opsForValue().get(CacheConstants.SYS_CONFIG_KEY + Constants.SYSTEM_TIME_CACHE_KEY);
        SysConfig config = new SysConfig();
        config.setConfigKey(Constants.SYSTEM_TIME_CACHE_KEY);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate date = LocalDate.parse(systemDate, formatter);
        LocalDate nextDay = date.plusDays(1);

        System.out.println("原日期：" + date);
        System.out.println("加1天：" + nextDay);

        config.setConfigValue(nextDay.format(formatter));
        config.setConfigId(100L);
        configService.updateConfig(config);
        System.out.println("定时任务--------系统时间更新完成");
        return R.ok();
    }
}
