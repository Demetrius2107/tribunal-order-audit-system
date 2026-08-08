package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.domain.model.AfterSale;
import com.demetrius.tribunal.order.domain.model.AfterSaleItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单应用层出参。
 */
public record AfterSaleResult(
        String id,
        String afterSaleNo,
        String orderId,
        String orderNo,
        String customerId,
        String type,
        String reason,
        String status,
        BigDecimal totalRefundAmount,
        String rejectReason,
        String refundTxnNo,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<ItemResult> items) {

    public record ItemResult(
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal refundAmount,
            BigDecimal depositRefund) {
    }

    public static AfterSaleResult from(AfterSale afterSale) {
        List<ItemResult> itemResults = afterSale.getItems().stream()
                .map(i -> new ItemResult(i.skuCode(), i.skuName(), i.quantity(),
                        i.refundAmount(), i.depositRefund()))
                .toList();
        return new AfterSaleResult(
                afterSale.getId(),
                afterSale.getAfterSaleNo(),
                afterSale.getOrderId(),
                afterSale.getOrderNo(),
                afterSale.getCustomerId(),
                afterSale.getType().name(),
                afterSale.getReason().name(),
                afterSale.getStatus().name(),
                afterSale.getTotalRefundAmount(),
                afterSale.getRejectReason(),
                afterSale.getRefundTxnNo(),
                afterSale.getCreateTime(),
                afterSale.getUpdateTime(),
                itemResults);
    }
}
