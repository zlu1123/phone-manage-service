package com.ruoyi.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.web.enums.PhoneType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ExternalApiService {

    @Autowired
    private RestTemplate restTemplate;
    @Value("${06api.key}")
    private String key;
    @Value("${06api.url}")
    private String url;
    @Value("${06api.test_flag:1}")
    private Boolean testFlag;

    public ApiResult fetchDataFromExternalApi(String type, String sn) {
        // 拼接请求地址：url + type + sn
        String reqUrl = url + "key=" + key + "&type=" + type + "&sn=" + sn;
        log.info("reqUrl: {}", reqUrl);
        String jsonString;
        if (testFlag) {
            jsonString = "{\"code\":0,\"data\":{\"serial\":\"F17FV5GA0DYP\",\"model\":\"iPhone 12\",\"thumbnail\":\"https:\\/\\/appleid.cdn-apple.com\\/static\\/deviceImages-15.0\\/iPhone\\/iPhone13,2-3b3b3c-47ade5\\/online-sourcelist__3x.png\",\"replaced\":false,\"registered\":true,\"activated\":true,\"validPurchaseDate\":true,\"warrantyDaysRemaining\":0,\"acEligible\":false,\"loaner\":\"unknown\",\"pre-activated\":true,\"warrantyYear\":1,\"warrantyStatus\":\"\\u5df2\\u8fc7\\u4fdd\\u4fee\\u671f\",\"appleCare\":false,\"estPurchaseDate\":\"2021-07-01\",\"repairExpiry\":\"2022-07-03\",\"color\":\"\\u84dd\\u8272\",\"storage\":\"128GB\",\"appleCareVerifyed\":true}}";
        }else {
            // 先获取原始字符串（不管 Content-Type）
            jsonString = restTemplate.getForObject(reqUrl, String.class);
        }
        log.info("原始响应: {}", jsonString);

        ApiResult result = new ApiResult();
        result.setRawJson(jsonString);
        // 手动反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ApiResponse response = objectMapper.readValue(jsonString, ApiResponse.class);
            if (response.getCode() == 0) {
                log.info("查询成功，data: {}", response.getData());
                result.setData(response.getData());
                result.setSuccess(true);
            } else {
                log.warn("查询失败，错误码: {}, 信息: {}", response.getCode(), response.getMessage());
            }
        } catch (Exception e) {
            log.error("JSON 解析失败", e);
        }
        return result;
    }

    // 定义响应实体类
    @Data
    public static class ApiResponse {
        private Integer code;
        private String message;
        private Object data;
    }

    @Data
    public static class ApiResult {
        private Object data;
        private String rawJson;
        private Boolean success;
    }
}