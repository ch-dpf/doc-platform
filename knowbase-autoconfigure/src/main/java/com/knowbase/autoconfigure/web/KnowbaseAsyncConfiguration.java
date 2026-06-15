package com.knowbase.autoconfigure.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class KnowbaseAsyncConfiguration {

    @Bean(name = "knowbaseTaskExecutor")
    public Executor knowbaseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("knowbase-async-");
        executor.initialize();
        return executor;
    }
}
