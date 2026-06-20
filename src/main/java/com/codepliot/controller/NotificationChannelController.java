package com.codepliot.controller;

import com.codepliot.model.NotificationChannelCreateRequest;
import com.codepliot.model.NotificationChannelVO;
import com.codepliot.model.NotificationSendResult;
import com.codepliot.model.Result;
import com.codepliot.service.notification.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知渠道控制器
 * <p>
 * 提供通知渠道的管理接口，包括渠道的创建、查询、连通性测试和删除等操作，
 * 用于配置消息推送的目标渠道（如飞书、邮件等）。
 * </p>
 */
@RestController
@RequestMapping("/api/notification/channels")
public class NotificationChannelController {

    private final NotificationService notificationService;

    public NotificationChannelController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 创建新的通知渠道
     *
     * @param request 渠道创建请求参数
     * @return 创建成功后的通知渠道视图对象
     */
    @PostMapping
    public Result<NotificationChannelVO> create(@Valid @RequestBody NotificationChannelCreateRequest request) {
        return Result.success("Notification channel created", notificationService.create(request));
    }

    /**
     * 获取当前用户的所有通知渠道列表
     *
     * @return 通知渠道列表
     */
    @GetMapping
    public Result<List<NotificationChannelVO>> list() {
        return Result.success(notificationService.listCurrentUserChannels());
    }

    /**
     * 测试指定通知渠道的连通性
     *
     * @param id 渠道 ID
     * @return 测试发送结果
     */
    @PostMapping("/{id}/test")
    public Result<NotificationSendResult> test(@PathVariable Long id) {
        return Result.success(notificationService.test(id));
    }

    /**
     * 删除指定的通知渠道
     *
     * @param id 渠道 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return Result.success("Notification channel deleted", null);
    }
}
