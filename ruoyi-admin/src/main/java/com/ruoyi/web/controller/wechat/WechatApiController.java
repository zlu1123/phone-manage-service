package com.ruoyi.web.controller.wechat;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.web.common.Constants;
import com.ruoyi.web.core.config.PhoneInfoConverterContext;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.domain.PhoneActiveInfo;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.IPhoneActiveInfoService;
import com.ruoyi.web.service.MapToObjectConverter;
import com.ruoyi.web.service.PhoneInfoConverter;
import com.ruoyi.web.service.impl.ExternalApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.ruoyi.common.utils.PageUtils.startPage;
import static com.ruoyi.common.utils.SecurityUtils.getUsername;


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
     * @param type 手机类型编号
     * @param code sn码
     * @return
     */
    @ApiOperation("查询激活信息")
    @GetMapping("/queryActiveInfo")
    public R queryActiveInfo(@RequestParam("typeCode") String typeCode, @RequestParam("code") String code) {
        String type = PhoneType.getValueByCode(typeCode);
        if (type == null) {
            return R.fail("查询失败，无效的手机类型！");
        };
        // 通过传入类型code
        ExternalApiService.ApiResult apiResult = externalApiService.fetchDataFromExternalApi(type, code);
        if (!apiResult.getSuccess()) {
            return R.fail("查询失败，请检查序号是否正确，或设备型号与序号是否匹配！");
        }
        // 存库，封装（需要的信息返回，其他原始数据以json形式存库）
        PhoneInfoDto phoneInfoDto = handleObject(typeCode, apiResult.getData());
        if (phoneInfoDto == null) {
            return R.fail("数据解析失败");
        }
        try {
            phoneInfoDto.setSystemTime((String) redisTemplate.opsForValue().get(Constants.SYSTEM_TIME_CACHE_KEY));
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
        PhoneActiveInfo info = new PhoneActiveInfo();
        info.setSn(dto.getSn());
        info.setImei1(dto.getImei1());
        info.setImei2(dto.getImei2());
        info.setModel(dto.getModel());
        info.setActivated(dto.getActivated());
        info.setActivateDate(dto.getActivateDate());
        info.setCoverage(dto.getCoverage());
        info.setActiveInfo(rawJson); // 原始完整 JSON
        info.setSysTime(dto.getSystemTime());
        // 设置创建人/更新人（如果自动填充未配置，可以手动设置）
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
}
