package com.ruoyi.quartz.task;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 定时任务调度测试
 * 
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ISysConfigService configService;

    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i)
    {
        System.out.println(StringUtils.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    public void ryParams(String params)
    {
        System.out.println("执行有参方法：" + params);
    }

    public void ryNoParams()
    {
        System.out.println("执行无参方法");
    }

    public void updateSystemDate()
    {
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
    }
}
