package com.codepliot.service.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskCancellationRegistry {

    private final ConcurrentMap<Long, Thread> runningThreads = new ConcurrentHashMap<>();

    public void register(Long taskId) {
        if (taskId != null) {
            runningThreads.put(taskId, Thread.currentThread());
        }
    }

    public void unregister(Long taskId) {
        if (taskId != null) {
            runningThreads.remove(taskId, Thread.currentThread());
        }
    }

    public void interrupt(Long taskId) {
        Thread thread = taskId == null ? null : runningThreads.get(taskId);
        if (thread != null) {
            thread.interrupt();
        }
    }
}
