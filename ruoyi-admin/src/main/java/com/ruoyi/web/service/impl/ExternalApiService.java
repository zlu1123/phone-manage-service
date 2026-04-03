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

    public Object fetchDataFromExternalApi(String type, String sn) {
        // 拼接请求地址：url + type + sn
        String reqUrl = url + "key=" + key + "&type=" + type + "&sn=" + sn;
        log.info("reqUrl: {}", reqUrl);
        // 先获取原始字符串（不管 Content-Type）
        String jsonString = restTemplate.getForObject(reqUrl, String.class);
        log.info("原始响应: {}", jsonString);

        // 手动反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ApiResponse response = objectMapper.readValue(jsonString, ApiResponse.class);
            if (response.getCode() == 0) {
                log.info("查询成功，data: {}", response.getData());
                return response.getData();
            } else {
                log.warn("查询失败，错误码: {}, 信息: {}", response.getCode(), response.getMessage());
            }
        } catch (Exception e) {
            log.error("JSON 解析失败", e);
        }
        return null;
    }

    // 定义响应实体类
    @Data
    public static class ApiResponse {
        private Integer code;
        private String message;
        private Object data;
    }
}