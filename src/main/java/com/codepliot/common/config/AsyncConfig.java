package com.codepliot.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步执行配置。
 * 当前项目仍是单体 MVP，任务执行又依赖本地 workspace 和 Lucene 索引，
 * 因此先使用 Spring Async + Redis 运行锁即可满足需求。
 * 后续如果演进到多实例或独立 worker 架构，再升级为 RabbitMQ / Redis Stream 更合适。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "agentTaskExecutor")
    public Executor agentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("agent-task-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
