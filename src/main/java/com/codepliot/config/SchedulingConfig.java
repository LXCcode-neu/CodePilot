package com.codepliot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务调度配置。
 * <p>
 * 通过 {@code @EnableScheduling} 启用 Spring 的定时任务调度能力，
 * 使项目中使用 {@code @Scheduled} 注解的方法能够按计划自动执行。
 * </p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
