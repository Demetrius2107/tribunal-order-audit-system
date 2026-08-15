package com.demetrius.tribunal.billing.interfaces.controller;

import com.demetrius.tribunal.common.auth.RequirePermission;
import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.billing.application.dto.ReconcileRecordPage;
import com.demetrius.tribunal.billing.application.service.ReconcileRecordQueryApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对账差异记录查询接口（对账结果产品化）。
 *
 * <p>提供财务对账任务写入的差异记录（t_reconcile_record）分页查询，
 * 支持按任务编码 / 差异类型 / 处理状态过滤。</p>
 */
@RestController
@RequestMapping("/api/reconcile/records")
public class ReconcileRecordController {

    private final ReconcileRecordQueryApplicationService reconcileRecordQueryApplicationService;

    public ReconcileRecordController(ReconcileRecordQueryApplicationService reconcileRecordQueryApplicationService) {
        this.reconcileRecordQueryApplicationService = reconcileRecordQueryApplicationService;
    }

    /**
     * 分页查询对账差异记录：GET /api/reconcile/records?taskCode=&recordType=&status=&pageNum=1&pageSize=20
     */
    @GetMapping
    @RequirePermission("reconcile:query")
    public ApiResponse<ReconcileRecordPage> query(
            @RequestParam(required = false) String taskCode,
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.ok(reconcileRecordQueryApplicationService.query(
                taskCode, recordType, status, pageNum, pageSize));
    }
}
