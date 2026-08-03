package com.demetrius.tribunal.task.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.task.application.service.TaskApplicationService;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务接口层（手动触发任务，运维用）。
 *
 * <p>TODO（学习任务）：任务执行历史查询。 </p>
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    /**
     * 手动触发任务：POST /api/tasks/{taskCode}/run
     */
    @PostMapping("/{taskCode}/run")
    public ApiResponse<Integer> run(@PathVariable String taskCode) {
        return ApiResponse.ok(taskApplicationService.runNow(taskCode));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
