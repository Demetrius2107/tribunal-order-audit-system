package com.demetrius.tribunal.notification.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.notification.application.dto.NotificationResult;
import com.demetrius.tribunal.notification.application.dto.NotificationSendCommand;
import com.demetrius.tribunal.notification.application.service.NotificationApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口层（REST，供各服务发送通知）。
 *
 * <p>TODO（学习任务）：模板管理接口（F-704）。</p>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * 发送通知：POST /api/notifications
     */
    @PostMapping
    public ApiResponse<NotificationResult> send(@Valid @RequestBody NotificationSendCommand command) {
        return ApiResponse.ok(notificationApplicationService.send(command));
    }

    /**
     * 查询通知：GET /api/notifications/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<NotificationResult> get(@PathVariable String id) {
        return ApiResponse.ok(notificationApplicationService.get(id));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
