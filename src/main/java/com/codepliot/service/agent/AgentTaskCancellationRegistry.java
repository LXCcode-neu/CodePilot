package com.codepliot.service.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * Agent 任务取消注册表。
 * <p>
 * 维护一个任务 ID 到执行线程的映射关系，用于支持任务取消操作。
 * 当任务开始执行时注册当前线程，任务完成或取消时注销，
 * 取消时可通过 {@link #interrupt(Long)} 中断对应的执行线程。
 * </p>
 */
@Service
public class AgentTaskCancellationRegistry {

    private final ConcurrentMap<Long, Thread> runningThreads = new ConcurrentHashMap<>();

    /**
     * 注册当前执行线程与任务 ID 的映射关系。
     *
     * @param taskId 任务 ID
     */
    public void register(Long taskId) {
        if (taskId != null) {
            runningThreads.put(taskId, Thread.currentThread());
        }
    }

    /**
     * 注销当前线程与任务 ID 的映射关系。
     *
     * @param taskId 任务 ID
     */
    public void unregister(Long taskId) {
        if (taskId != null) {
            runningThreads.remove(taskId, Thread.currentThread());
        }
    }

    /**
     * 中断指定任务对应的执行线程。
     *
     * @param taskId 任务 ID
     */
    public void interrupt(Long taskId) {
        Thread thread = taskId == null ? null : runningThreads.get(taskId);
        if (thread != null) {
            thread.interrupt();
        }
    }
}
