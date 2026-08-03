package com.demetrius.tribunal.financesettlement.interfaces.facade;

import com.demetrius.tribunal.financesettlement.application.service.RefundApplicationService;
import com.demetrius.tribunal.financesettlement.common.dto.RefundApplyRequest;
import com.demetrius.tribunal.financesettlement.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款申请接口（PRD 4.4 POST /api/v1/refund/apply）。
 */
@RestController
@RequestMapping("/api/v1/refund")
public class RefundController {

    private final RefundApplicationService refundApplicationService;

    public RefundController(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    /**
     * 退款申请（大额自动进入人工审核 PENDING）。
     */
    @PostMapping("/apply")
    public ApiResponse<Void> apply(@Valid @RequestBody RefundApplyRequest request) {
        refundApplicationService.apply(request);
        return ApiResponse.ok(null);
    }
}
