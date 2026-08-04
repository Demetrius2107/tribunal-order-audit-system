package com.demetrius.tribunal.order.interfaces.controller;

import com.demetrius.tribunal.order.application.dto.OrderCreateCommand;
import com.demetrius.tribunal.order.application.dto.OrderPageResult;
import com.demetrius.tribunal.order.application.dto.OrderResult;
import com.demetrius.tribunal.order.application.dto.OrderReviewCommand;
import com.demetrius.tribunal.order.application.service.OrderApplicationService;
import com.demetrius.tribunal.order.application.service.OrderReviewApplicationService;
import com.demetrius.tribunal.order.interfaces.dto.OrderCreateRequest;
import com.demetrius.tribunal.order.interfaces.dto.OrderModifyRequest;
import com.demetrius.tribunal.order.interfaces.dto.OrderReviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单接口层（REST 控制器）。
 *
 * <p>参照通用做法。</p>
 *
 * <p>接口层职责（最薄）：</p>
 * <ol>
 *   <li>接收 HTTP 请求与参数校验（@Valid）</li>
 *   <li>接口 DTO → 应用层 Command 的转换</li>
 *   <li>返回应用层结果</li>
 * </ol>
 * <p>接口层不包含业务规则、不直接触碰领域对象/仓储。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>统一响应体封装（参照通用做法</li>
 *   <li>全局异常处理（@RestControllerAdvice，参照通用做法</li>
 *   <li>操作日志注解（参照通用做法</li>
 *   <li>防重复提交注解（参照通用做法，里程碑 4）</li>
 *   <li>分页查询接口（参照通用做法</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    private final OrderReviewApplicationService reviewApplicationService;

    public OrderController(OrderApplicationService orderApplicationService,
                           OrderReviewApplicationService reviewApplicationService) {
        this.orderApplicationService = orderApplicationService;
        this.reviewApplicationService = reviewApplicationService;
    }

    /**
     * 下单：POST /api/orders
     */
    @PostMapping
    public OrderResult create(@Valid @RequestBody OrderCreateRequest request) {
        OrderCreateCommand command = new OrderCreateCommand(
                request.customerId(),
                request.skus().stream()
                        .map(s -> new OrderCreateCommand.SkuItem(
                                s.skuCode(), s.skuName(), s.quantity(), s.price()))
                        .toList(),
                request.palletSpecs(),
                request.orderType(),
                request.carPooling(),
                request.returnablePackagings() == null ? List.of() : request.returnablePackagings().stream()
                        .map(r -> new OrderCreateCommand.ReturnableItem(
                                r.packagingType(), r.packagingName(), r.quantity(), r.unitDeposit()))
                        .toList(),
                request.discountPoolDeduction(),
                request.shippingFee(),
                request.depositConfigBySku());
        return orderApplicationService.createOrder(command);
    }

    /**
     * 查询订单：GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public OrderResult get(@PathVariable String orderId) {
        return orderApplicationService.getOrder(orderId);
    }

    /**
     * 分页查询订单列表：GET /api/orders?customerId=&status=&pageNum=1&pageSize=10
     */
    @GetMapping
    public OrderPageResult list(@RequestParam(required = false) String customerId,
                                @RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize) {
        return orderApplicationService.listOrders(customerId, status, pageNum, pageSize);
    }

    /**
     * 审单：POST /api/orders/{orderId}/review
     */
    @PostMapping("/{orderId}/review")
    public OrderResult review(@PathVariable String orderId,
                              @Valid @RequestBody OrderReviewRequest request) {
        OrderReviewCommand command = new OrderReviewCommand(
                orderId, request.approved(), request.reason(), request.operator());
        return reviewApplicationService.review(command);
    }

    /**
     * 修改订单：PUT /api/orders/{orderId}（F-309，仅待确认状态可改，替换明细并重算金额）
     */
    @PutMapping("/{orderId}")
    public OrderResult modify(@PathVariable String orderId,
                              @Valid @RequestBody OrderModifyRequest request) {
        List<OrderCreateCommand.SkuItem> items = request.skus().stream()
                .map(s -> new OrderCreateCommand.SkuItem(
                        s.skuCode(), s.skuName(), s.quantity(), s.price()))
                .toList();
        return orderApplicationService.modifyOrder(orderId, items, request.palletSpecs());
    }

    /**
     * 取消订单：POST /api/orders/{orderId}/cancel（释放信用预占，F-403）。
     */
    @PostMapping("/{orderId}/cancel")
    public OrderResult cancel(@PathVariable String orderId) {
        return orderApplicationService.cancelOrder(orderId);
    }

    /**
     * 心跳接口（参照通用做法，用于运维探活）。
     */
    @GetMapping("/heartbeat")
    public Map<String, String> heartbeat() {
        return Map.of("status", "UP");
    }
}
