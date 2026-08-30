package com.ruoyi.web.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 订单导入异步任务线程池
 * <p>
 * 单线程执行：同一时间只允许一个导入任务运行（任务提交前有进行中任务守卫，
 * 线程池维度再兜底一层），AbortPolicy 保证线程池满载时立即失败并反馈用户，
 * 不使用 CallerRunsPolicy（避免导入逻辑回退到请求线程阻塞响应）。
 */
@Configuration
public class ImportTaskExecutorConfig {

    @Bean(name = "importTaskExecutor")
    public ThreadPoolTaskExecutor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("import-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
