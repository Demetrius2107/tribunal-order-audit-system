package com.demetrius.tribunal.financesettlement.interfaces.facade;

import com.demetrius.tribunal.financesettlement.application.service.SettlementApplicationService;
import com.demetrius.tribunal.financesettlement.common.dto.ChargeRequest;
import com.demetrius.tribunal.financesettlement.common.dto.SettlementView;
import com.demetrius.tribunal.financesettlement.common.dto.SplitRequest;
import com.demetrius.tribunal.financesettlement.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付扣款接口（PRD 4.2 POST /api/v1/payment/charge）。
 */
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final SettlementApplicationService settlementApplicationService;

    public PaymentController(SettlementApplicationService settlementApplicationService) {
        this.settlementApplicationService = settlementApplicationService;
    }

    /**
     * 幂等扣款（结算单号 + 扣款批次号幂等，杜绝重复扣款）。
     */
    @PostMapping("/charge")
    public ApiResponse<SettlementView> charge(@Valid @RequestBody ChargeRequest request) {
        return ApiResponse.ok(settlementApplicationService.charge(request));
    }

    /**
     * 分账执行（PRD 4.3 POST /api/v1/settlement/split，扣款成功后拆分资金到各分账方）。
     */
    @PostMapping("/split")
    public ApiResponse<Void> split(@RequestBody SplitRequest request) {
        settlementApplicationService.split(request);
        return ApiResponse.ok(null);
    }
}
