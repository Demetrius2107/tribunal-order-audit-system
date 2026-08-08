package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.common.auth.RequirePermission;
import com.demetrius.tribunal.order.application.dto.AfterSaleResult;
import com.demetrius.tribunal.order.application.service.AfterSaleApplicationService;
import com.demetrius.tribunal.order.domain.model.AfterSale;
import com.demetrius.tribunal.order.interfaces.dto.AfterSaleCreateRequest;
import com.demetrius.tribunal.order.interfaces.dto.AfterSaleReviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 售后接口层（REST 控制器）。
 *
 * <p>提供售后退货全流程 REST API：</p>
 * <ul>
 *   <li>POST   /api/aftersales           - 发起售后申请</li>
 *   <li>POST   /api/aftersales/{id}/review - 审核售后单</li>
 *   <li>POST   /api/aftersales/{id}/confirm - 确认收货并退款（退货退款专用）</li>
 *   <li>GET    /api/aftersales/{id}       - 查询售后单详情</li>
 *   <li>GET    /api/aftersales?orderId=   - 按订单查询售后列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/aftersales")
public class AfterSaleController {

    private final AfterSaleApplicationService afterSaleApplicationService;

    public AfterSaleController(AfterSaleApplicationService afterSaleApplicationService) {
        this.afterSaleApplicationService = afterSaleApplicationService;
    }

    /**
     * 发起售后申请：POST /api/aftersales（需权限 aftersale:create）
     */
    @PostMapping
    @RequirePermission("aftersale:create")
    public AfterSaleResult create(@Valid @RequestBody AfterSaleCreateRequest request) {
        List<AfterSale.ReturnRequest> returnItems = request.items().stream()
                .map(i -> new AfterSale.ReturnRequest(i.skuCode(), i.quantity()))
                .toList();
        return afterSaleApplicationService.createAfterSale(
                request.orderId(), request.type(), request.reason(), returnItems);
    }

    /**
     * 审核售后单：POST /api/aftersales/{id}/review（需权限 aftersale:review）
     */
    @PostMapping("/{id}/review")
    @RequirePermission("aftersale:review")
    public AfterSaleResult review(@PathVariable String id,
                                  @Valid @RequestBody AfterSaleReviewRequest request) {
        return afterSaleApplicationService.review(id, request.approved(), request.reason());
    }

    /**
     * 确认收货并退款：POST /api/aftersales/{id}/confirm（需权限 aftersale:review）
     *
     * <p>仓库收到退回商品后调用，执行库存回滚 + 退款。</p>
     */
    @PostMapping("/{id}/confirm")
    @RequirePermission("aftersale:review")
    public AfterSaleResult confirmReceipt(@PathVariable String id) {
        return afterSaleApplicationService.confirmReceiptAndRefund(id);
    }

    /**
     * 查询售后单详情：GET /api/aftersales/{id}（需权限 aftersale:view）
     */
    @GetMapping("/{id}")
    @RequirePermission("aftersale:view")
    public AfterSaleResult get(@PathVariable String id) {
        return afterSaleApplicationService.getAfterSale(id);
    }

    /**
     * 按订单查询售后列表：GET /api/aftersales?orderId=xxx（需权限 aftersale:view）
     */
    @GetMapping
    @RequirePermission("aftersale:view")
    public List<AfterSaleResult> listByOrder(@RequestParam String orderId) {
        return afterSaleApplicationService.listByOrder(orderId);
    }
}
