package com.snb.deal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author lizengqiang
 * @Description 线程池配置
 * @date 2018/4/11 13:54
 */
@Configuration
@EnableAsync
public class ExecutePoolTask {
    @Bean
    public Executor orderRedeemAsyncPool(Environment environment) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(environment.getProperty("threadPool.redeem.corePoolSize", Integer.class, 5));
        executor.setMaxPoolSize(environment.getProperty("threadPool.redeem.maxPoolSize", Integer.class, 10));
        executor.setQueueCapacity(environment.getProperty("threadPool.redeem.queueCapacity", Integer.class, 100));
        executor.setKeepAliveSeconds(environment.getProperty("threadPool.redeem.keepAliveSeconds", Integer.class, 10));
        executor.setThreadNamePrefix("OrderRedeemExecutor-");
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }
}
