package com.codepliot.controller;

import com.codepliot.model.NotificationChannelCreateRequest;
import com.codepliot.model.NotificationChannelVO;
import com.codepliot.model.NotificationSendResult;
import com.codepliot.model.Result;
import com.codepliot.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification/channels")
public class NotificationChannelController {

    private final NotificationService notificationService;

    public NotificationChannelController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public Result<NotificationChannelVO> create(@Valid @RequestBody NotificationChannelCreateRequest request) {
        return Result.success("Notification channel created", notificationService.create(request));
    }

    @GetMapping
    public Result<List<NotificationChannelVO>> list() {
        return Result.success(notificationService.listCurrentUserChannels());
    }

    @PostMapping("/{id}/test")
    public Result<NotificationSendResult> test(@PathVariable Long id) {
        return Result.success(notificationService.test(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return Result.success("Notification channel deleted", null);
    }
}
