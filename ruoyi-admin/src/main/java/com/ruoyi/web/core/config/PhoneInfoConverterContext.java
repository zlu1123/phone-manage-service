package com.ruoyi.web.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.web.enums.PhoneType;
import com.ruoyi.web.model.ApplePhoneObject;
import com.ruoyi.web.model.HuaWeiPhoneObject;
import com.ruoyi.web.model.XiaoMiPhoneObject;
import com.ruoyi.web.service.MapToObjectConverter;
import com.ruoyi.web.service.PhoneInfoConverter;
import com.ruoyi.web.service.impl.ApplePhoneConverter;
import com.ruoyi.web.service.impl.HuaWeiPhoneConverter;
import com.ruoyi.web.service.impl.XiaoMiPhoneConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PhoneInfoConverterContext {
    private final Map<String, PhoneInfoConverter> converterMap = new ConcurrentHashMap<>();
    private final Map<String, MapToObjectConverter> objectConverterMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 注册策略
     */
    public void registerStrategy(String type, PhoneInfoConverter converter) {
        converterMap.put(type, converter);
    }

    public void registerObjectConverter(String type, MapToObjectConverter converter) {
        objectConverterMap.put(type, converter);
    }

    /**
     * 获取策略
     */
    public PhoneInfoConverter getConverter(String type) {
        return converterMap.get(type);
    }

    public MapToObjectConverter getObjectConverter(String type) {
        return objectConverterMap.get(type);
    }
    /**
     * 初始化，注册所有已知的策略
     */
    @PostConstruct
    public void init() {
        // 注册对象转换器（Map → 具体类型）
        registerObjectConverter(PhoneType.APPLE.getCode(),map -> objectMapper.convertValue(map, ApplePhoneObject.class));
        registerObjectConverter(PhoneType.XIAOMI.getCode(), map -> objectMapper.convertValue(map, XiaoMiPhoneObject.class));
        registerObjectConverter(PhoneType.HUAWEI.getCode(), map -> objectMapper.convertValue(map, HuaWeiPhoneObject.class));
        // 荣耀已从华为接口拆分，但返回结构与华为一致，复用 HuaWeiPhoneObject 解析
        registerObjectConverter(PhoneType.HONOR.getCode(), map -> objectMapper.convertValue(map, HuaWeiPhoneObject.class));

        registerStrategy(PhoneType.APPLE.getCode(), new ApplePhoneConverter());
        registerStrategy(PhoneType.XIAOMI.getCode(), new XiaoMiPhoneConverter());
        registerStrategy(PhoneType.HUAWEI.getCode(), new HuaWeiPhoneConverter());
        // 荣耀复用华为字段映射
        registerStrategy(PhoneType.HONOR.getCode(), new HuaWeiPhoneConverter());
        // 可以继续添加更多类型...
    }
}
