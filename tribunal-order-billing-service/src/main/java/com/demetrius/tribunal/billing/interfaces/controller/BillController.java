package com.demetrius.tribunal.billing.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.billing.application.dto.BillPageResult;
import com.demetrius.tribunal.billing.application.dto.BillPaymentResult;
import com.demetrius.tribunal.billing.application.dto.BillReceiveCommand;
import com.demetrius.tribunal.billing.application.dto.BillResult;
import com.demetrius.tribunal.billing.application.service.BillingApplicationService;
import com.demetrius.tribunal.billing.application.service.BillQueryApplicationService;
import com.demetrius.tribunal.billing.interfaces.dto.BillReceiveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 金融账单接口层（REST 控制器，供 订单服务 跨服务调用 + 账单 内部履约操作）。
 *
 * <p>对应需求：F-307（接收转单）、F-503（发货/签收回传）。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>履约操作鉴权（账单 内部操作与 订单服务 调用分离）</li>
 *   <li>部分发货/部分签收接口</li>
 *   <li>履约列表分页查询</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillingApplicationService billingApplicationService;

    private final BillQueryApplicationService billQueryApplicationService;

    public BillController(BillingApplicationService billingApplicationService,
                          BillQueryApplicationService billQueryApplicationService) {
        this.billingApplicationService = billingApplicationService;
        this.billQueryApplicationService = billQueryApplicationService;
    }

    /**
     * 接收转单生成账单：POST /api/bills
     */
    @PostMapping
    public ApiResponse<BillResult> generate(@Valid @RequestBody BillReceiveRequest request) {
        BillReceiveCommand command = new BillReceiveCommand(
                request.sourceOrderNo(),
                request.customerId(),
                request.lines().stream()
                        .map(l -> new BillReceiveCommand.BillLineItem(
                                l.skuCode(), l.skuName(), l.quantity(), l.price()))
                        .toList());
        return ApiResponse.ok(billingApplicationService.generateBill(command));
    }

    /**
     * 查询账单：GET /api/bills/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<BillResult> get(@PathVariable String id) {
        return ApiResponse.ok(billingApplicationService.getBill(id));
    }

    /**
     * 账单分页列表（对外报表）：GET /api/bills?status=&customerId=&pageNum=1&pageSize=10
     */
    @GetMapping
    public ApiResponse<BillPageResult> list(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String customerId,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(billQueryApplicationService.pageBills(status, customerId, pageNum, pageSize));
    }

    /**
     * 账单收款流水明细（对外报表）：GET /api/bills/{id}/payments
     */
    @GetMapping("/{id}/payments")
    public ApiResponse<List<BillPaymentResult>> payments(@PathVariable String id) {
        return ApiResponse.ok(billQueryApplicationService.listPayments(id));
    }

    /**
     * 按上游订单编号查询账单（对账任务用）：GET /api/bills/by-order/{sourceOrderNo}
     */
    @GetMapping("/by-order/{sourceOrderNo}")
    public ApiResponse<BillResult> getBySourceOrderNo(@PathVariable String sourceOrderNo) {
        return ApiResponse.ok(billingApplicationService.getBillBySourceOrderNo(sourceOrderNo));
    }

    /**
     * 确认：POST /api/bills/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<BillResult> confirm(@PathVariable String id) {
        return ApiResponse.ok(billingApplicationService.confirm(id));
    }

    /**
     * 结算：POST /api/bills/{id}/settle
     */
    @PostMapping("/{id}/settle")
    public ApiResponse<BillResult> settle(@PathVariable String id) {
        return ApiResponse.ok(billingApplicationService.settle(id));
    }

    /**
     * 核销：POST /api/bills/{id}/verify
     */
    @PostMapping("/{id}/verify")
    public ApiResponse<BillResult> verify(@PathVariable String id) {
        return ApiResponse.ok(billingApplicationService.verify(id));
    }

    /**
     * 取消：POST /api/bills/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<BillResult> cancel(@PathVariable String id) {
        return ApiResponse.ok(billingApplicationService.cancel(id));
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
