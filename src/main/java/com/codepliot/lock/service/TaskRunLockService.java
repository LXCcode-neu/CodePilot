package com.codepliot.lock.service;

import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 任务运行锁服务。
 * 基于 Redis 防止同一个任务被重复触发执行。
 */
@Service
public class TaskRunLockService {

    private static final String TASK_RUNNING_KEY_PREFIX = "codepilot:task:running:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;

    public TaskRunLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试为某个任务加运行锁。
     * 加锁成功时返回锁值，失败时抛出“任务正在运行”异常。
     */
    public String lock(Long taskId) {
        validateTaskId(taskId);
        String lockKey = buildLockKey(taskId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.AGENT_TASK_RUNNING, "任务正在运行");
        }
        return lockValue;
    }

    /**
     * 释放任务运行锁。
     * 只有锁值匹配时才删除，避免误删其他请求刚刚续上的锁。
     */
    public void unlock(Long taskId, String lockValue) {
        if (taskId == null || taskId <= 0 || lockValue == null || lockValue.isBlank()) {
            return;
        }
        String lockKey = buildLockKey(taskId);
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    public String buildLockKey(Long taskId) {
        validateTaskId(taskId);
        return TASK_RUNNING_KEY_PREFIX + taskId;
    }

    public Duration lockTtl() {
        return LOCK_TTL;
    }

    private void validateTaskId(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "taskId must be greater than 0");
        }
    }
}
