package com.ruoyi.web.controller.wechat;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.core.config.PhoneInfoConverterContext;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.model.PhoneInfoDto;
import com.ruoyi.web.service.MapToObjectConverter;
import com.ruoyi.web.service.PhoneInfoConverter;
import com.ruoyi.web.service.impl.ExternalApiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Api("小程序接口")
@Slf4j
@RestController
@RequestMapping("/wechat/api/")
public class WechatApiController {

    @Autowired
    private ExternalApiService externalApiService;
    @Autowired
    private PhoneInfoConverterContext converterContext;

    @Value("${06api.test_flag:1}")
    private Boolean testFlag;

    /**
     * @param type 手机类型编号
     * @param code sn码
     * @return
     */
    @ApiOperation("查询激活信息")
    @GetMapping("/queryActiveInfo")
    public R queryActiveInfo(@RequestParam("typeCode") String typeCode, @RequestParam("code") String code) {
        if(testFlag){
            PhoneInfoDto dto = new PhoneInfoDto();
            dto.setSn(code);
            dto.setModel("iPhone 12");
            dto.setActivated(true);
            dto.setActivateDate("2021-07-01");
            dto.setCoverage("2022-07-03");
            return R.ok(dto);
        }
        String type = PhoneType.getValueByCode(typeCode);
        if (type == null) {
            return R.fail("查询失败，无效的手机类型！");
        };
        // 通过传入类型code
        Object object = externalApiService.fetchDataFromExternalApi(type, code);
        if (object == null) {
            return R.fail("查询失败，请检查序号是否正确，或设备型号与序号是否匹配！");
        }
        // 存库，封装成
        PhoneInfoDto phoneInfoDto = handleObject(typeCode, object);
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
}
