package com.codepliot.service.task;

import com.codepliot.config.TaskRunLockProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * 任务运行锁服务。
 *
 * <p>基于 Redis 为单个任务提供互斥运行保护，避免同一任务在短时间内被重复触发。
 */
@Service
public class TaskRunLockService {

    private static final String TASK_RUNNING_KEY_PREFIX = "codepilot:task:running:";

    /**
     * 通过 Lua 保证“比对锁值并删除”在 Redis 侧原子执行，避免误删其他请求刚加上的新锁。
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final TaskRunLockProperties taskRunLockProperties;

    /**
     * 创建任务运行锁服务。
     */
    public TaskRunLockService(StringRedisTemplate stringRedisTemplate,
                              TaskRunLockProperties taskRunLockProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.taskRunLockProperties = taskRunLockProperties;
    }

    /**
     * 为任务申请运行锁。
     *
     * <p>使用 setIfAbsent 语义保证只有第一个请求能成功拿到锁。
     */
    public String lock(Long taskId) {
        validateTaskId(taskId);
        String lockKey = buildLockKey(taskId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, taskRunLockProperties.getTtl());
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.AGENT_TASK_RUNNING, "任务正在运行");
        }
        return lockValue;
    }

    /**
     * 释放任务运行锁。
     *
     * <p>只有锁值匹配时才会删除，避免误删其他请求重新创建的锁。
     */
    public void unlock(Long taskId, String lockValue) {
        if (taskId == null || taskId <= 0 || lockValue == null || lockValue.isBlank()) {
            return;
        }
        String lockKey = buildLockKey(taskId);
        stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
    }

    /**
     * 构建任务运行锁的 Redis key。
     */
    public String buildLockKey(Long taskId) {
        validateTaskId(taskId);
        return TASK_RUNNING_KEY_PREFIX + taskId;
    }

    /**
     * 返回当前锁的过期时间配置。
     */
    public Duration lockTtl() {
        return taskRunLockProperties.getTtl();
    }

    /**
     * 校验任务 ID 是否合法。
     */
    private void validateTaskId(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "taskId must be greater than 0");
        }
    }
}