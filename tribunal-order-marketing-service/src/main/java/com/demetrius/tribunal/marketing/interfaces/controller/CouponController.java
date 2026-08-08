package com.demetrius.tribunal.marketing.interfaces.controller;

import com.demetrius.tribunal.common.response.ApiResponse;
import com.demetrius.tribunal.marketing.application.dto.CouponTemplateResult;
import com.demetrius.tribunal.marketing.application.dto.UserCouponResult;
import com.demetrius.tribunal.marketing.application.service.CouponApplicationService;
import com.demetrius.tribunal.marketing.interfaces.dto.CouponOperateRequest;
import com.demetrius.tribunal.marketing.interfaces.dto.CouponReceiveRequest;
import com.demetrius.tribunal.marketing.interfaces.dto.CouponTemplateCreateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券接口层（REST）。
 *
 * <p>对应需求：F-210（优惠券：模板管理/领券/核销/防刷/过期回收）。</p>
 *
 * <p>路由：/api/coupons/**（网关已配置转发到 marketing-service）。</p>
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponApplicationService couponApplicationService;

    public CouponController(CouponApplicationService couponApplicationService) {
        this.couponApplicationService = couponApplicationService;
    }

    // ===== 券模板管理 =====

    /** 创建券模板：POST /api/coupons/templates */
    @PostMapping("/templates")
    public ApiResponse<CouponTemplateResult> createTemplate(
            @Valid @RequestBody CouponTemplateCreateRequest request) {
        return ApiResponse.ok(couponApplicationService.createTemplate(
                request.name(), request.type(),
                request.threshold(), request.deductionAmount(), request.discountRate(),
                request.totalQuota(), request.perUserLimit() == null ? 1 : request.perUserLimit(),
                request.validStartTime(), request.validEndTime()));
    }

    /** 查询券模板详情：GET /api/coupons/templates/{templateId} */
    @GetMapping("/templates/{templateId}")
    public ApiResponse<CouponTemplateResult> getTemplate(@PathVariable String templateId) {
        return ApiResponse.ok(couponApplicationService.getTemplate(templateId));
    }

    /** 查询所有可领的券模板：GET /api/coupons/templates */
    @GetMapping("/templates")
    public ApiResponse<List<CouponTemplateResult>> listActiveTemplates() {
        return ApiResponse.ok(couponApplicationService.listActiveTemplates());
    }

    /** 停用券模板：POST /api/coupons/templates/{templateId}/deactivate */
    @PostMapping("/templates/{templateId}/deactivate")
    public ApiResponse<CouponTemplateResult> deactivateTemplate(@PathVariable String templateId) {
        return ApiResponse.ok(couponApplicationService.deactivateTemplate(templateId));
    }

    // ===== 领券 =====

    /** 领券：POST /api/coupons/templates/{templateId}/receive */
    @PostMapping("/templates/{templateId}/receive")
    public ApiResponse<UserCouponResult> receive(@PathVariable String templateId,
                                                 @Valid @RequestBody CouponReceiveRequest request) {
        return ApiResponse.ok(couponApplicationService.receive(templateId, request.customerId()));
    }

    // ===== 核销操作（锁定/释放/核销） =====

    /** 锁定券（下单预占）：POST /api/coupons/lock */
    @PostMapping("/lock")
    public ApiResponse<UserCouponResult> lock(@Valid @RequestBody CouponOperateRequest request) {
        return ApiResponse.ok(couponApplicationService.lock(request.couponCode(), request.orderId()));
    }

    /** 释放券（订单取消回滚）：POST /api/coupons/release */
    @PostMapping("/release")
    public ApiResponse<UserCouponResult> release(@Valid @RequestBody CouponOperateRequest request) {
        return ApiResponse.ok(couponApplicationService.release(request.couponCode()));
    }

    /** 核销券（支付成功）：POST /api/coupons/use */
    @PostMapping("/use")
    public ApiResponse<UserCouponResult> use(@Valid @RequestBody CouponOperateRequest request) {
        return ApiResponse.ok(couponApplicationService.use(request.couponCode(), request.orderId()));
    }

    // ===== 试算 =====

    /** 试算券抵扣金额：GET /api/coupons/discount?couponCode=..&orderAmount=.. */
    @GetMapping("/discount")
    public ApiResponse<BigDecimal> calculateDiscount(@RequestParam String couponCode,
                                                     @RequestParam BigDecimal orderAmount) {
        return ApiResponse.ok(couponApplicationService.calculateDiscount(couponCode, orderAmount));
    }

    // ===== 查询 =====

    /** 按券码查询用户券：GET /api/coupons/{couponCode} */
    @GetMapping("/{couponCode}")
    public ApiResponse<UserCouponResult> getCoupon(@PathVariable String couponCode) {
        return ApiResponse.ok(couponApplicationService.getUserCoupon(couponCode));
    }

    /** 查询用户的券列表：GET /api/coupons/customer/{customerId}?status=AVAILABLE */
    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<UserCouponResult>> listByCustomer(@PathVariable String customerId,
                                                              @RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return ApiResponse.ok(couponApplicationService.listByCustomer(customerId));
        }
        return ApiResponse.ok(couponApplicationService.listByCustomerAndStatus(customerId, status));
    }

    // ===== 过期回收 =====

    /** 手动触发过期券回收（运维/定时任务兜底）：POST /api/coupons/expire-scan */
    @PostMapping("/expire-scan")
    public ApiResponse<Integer> expireScan() {
        return ApiResponse.ok(couponApplicationService.expireCoupons());
    }
}
