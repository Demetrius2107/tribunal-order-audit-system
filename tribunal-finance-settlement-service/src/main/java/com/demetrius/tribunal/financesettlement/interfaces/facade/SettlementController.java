package com.demetrius.tribunal.financesettlement.interfaces.facade;

import com.demetrius.tribunal.financesettlement.application.service.SettlementApplicationService;
import com.demetrius.tribunal.financesettlement.common.dto.SettlementView;
import com.demetrius.tribunal.financesettlement.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算单接口（查询结算单状态，PRD 2.5 结算打款层）。
 */
@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final SettlementApplicationService settlementApplicationService;

    public SettlementController(SettlementApplicationService settlementApplicationService) {
        this.settlementApplicationService = settlementApplicationService;
    }

    /**
     * 心跳接口（运维探活）。
     */
    @GetMapping("/heartbeat")
    public ApiResponse<String> heartbeat() {
        return ApiResponse.ok("UP");
    }
}
