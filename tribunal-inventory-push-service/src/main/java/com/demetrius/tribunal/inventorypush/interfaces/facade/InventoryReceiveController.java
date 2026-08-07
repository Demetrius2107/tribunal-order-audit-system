package com.demetrius.tribunal.inventorypush.interfaces.facade;

import com.demetrius.tribunal.inventorypush.application.service.InventoryReceiveApplicationService;
import com.demetrius.tribunal.common.dto.inventory.InventoryReceiveRequest;
import com.demetrius.tribunal.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存接收接口（上游推送入口，PRD 4.1 POST /api/v1/inventory/receive）。
 *
 * <p>基建说明：API Key / HMAC-SHA256 签名 / IP 白名单鉴权（PRD 2.1.2 FR-005/006）
 * 建议通过 Filter/Interceptor 统一实现，留待后续填充。</p>
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryReceiveController {

    private final InventoryReceiveApplicationService receiveApplicationService;

    public InventoryReceiveController(InventoryReceiveApplicationService receiveApplicationService) {
        this.receiveApplicationService = receiveApplicationService;
    }

    /**
     * 接收上游库存推送（支持全量/增量，幂等接收）。
     */
    @PostMapping("/receive")
    public ApiResponse<Void> receive(@Valid @RequestBody InventoryReceiveRequest request) {
        receiveApplicationService.receive(request);
        return ApiResponse.ok(null);
    }
}
