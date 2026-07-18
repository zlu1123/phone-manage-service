package com.ruoyi.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.web.enums.PhoneType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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

    public ApiResult fetchDataFromExternalApi(String type, String code) {
        return fetchDataFromExternalApi(type, code, false);
    }

    /**
     * 查询06API
     * @param type  手机品牌类型
     * @param code  SN或IMEI
     * @param isImei true=用IMEI参数名查询，false=用SN参数名查询
     */
    public ApiResult fetchDataFromExternalApi(String type, String code, boolean isImei) {
        // 拼接请求地址：IMEI优先品牌（vivo/oppo）需要用 &imei= 参数
        String paramName = isImei ? "&imei=" : "&sn=";
        String reqUrl = url + "key=" + key + "&type=" + type + paramName + code;
        log.info("reqUrl: {}", reqUrl);
        String jsonString;
        if (testFlag) {
            jsonString = "{\"code\":0,\"data\":{\"serial\":\"F17FV5GA0DYP\",\"model\":\"iPhone 12\",\"thumbnail\":\"https:\\/\\/appleid.cdn-apple.com\\/static\\/deviceImages-15.0\\/iPhone\\/iPhone13,2-3b3b3c-47ade5\\/online-sourcelist__3x.png\",\"replaced\":false,\"registered\":true,\"activated\":true,\"validPurchaseDate\":true,\"warrantyDaysRemaining\":0,\"acEligible\":false,\"loaner\":\"unknown\",\"pre-activated\":true,\"warrantyYear\":1,\"warrantyStatus\":\"\\u5df2\\u8fc7\\u4fdd\\u4fee\\u671f\",\"appleCare\":false,\"estPurchaseDate\":\"2021-07-01\",\"repairExpiry\":\"2022-07-03\",\"color\":\"\\u84dd\\u8272\",\"storage\":\"128GB\",\"appleCareVerifyed\":true}}";
        }else {
            // 使用 POST 请求（06API 官方文档要求 POST 方式）
            jsonString = restTemplate.exchange(reqUrl, HttpMethod.POST, HttpEntity.EMPTY, String.class).getBody();
        }
        log.info("原始响应: {}", jsonString);
        return handleData(jsonString);
    }

    /**
     * 查询账户余额
     * @return
     */
    public ApiResult getBalance() {
        String reqUrl = url + "key=" + key + "&type=balance";
        log.info("reqUrl: {}", reqUrl);
        String jsonString = restTemplate.exchange(reqUrl, HttpMethod.POST, HttpEntity.EMPTY, String.class).getBody();
        log.info("原始响应: {}", jsonString);
        return handleData(jsonString);
    }

    private ApiResult handleData(String jsonString){
        ApiResult result = new ApiResult();
        result.setRawJson(jsonString);
        // 手动反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ApiResponse response = objectMapper.readValue(jsonString, ApiResponse.class);
            if (response.getCode() == 0) {
                log.info("查询成功，data: {}", response.getData());
                result.setSuccess(true);
                result.setData(response.getData());
            } else {
                result.setSuccess(false);
                result.setData("错误码：" + response.getCode() + "，错误信息：" + response.getMessage());
                log.warn("查询失败，错误码: {}, 信息: {}", response.getCode(), response.getMessage());
            }
        } catch (Exception e) {
            log.error("JSON 解析失败，原始响应: {}", jsonString, e);
            result.setSuccess(false);
            result.setData("查询失败，API返回异常：" + jsonString);
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
        private Boolean success = false;
    }
}