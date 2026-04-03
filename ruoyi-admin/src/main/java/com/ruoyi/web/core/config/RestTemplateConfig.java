package com.ruoyi.web.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时：单位毫秒
        factory.setConnectTimeout(5000);   // 5秒
        // 读取超时（Socket超时）
        factory.setReadTimeout(30*1000);     // 30秒
        return new RestTemplate(factory);
    }
}
