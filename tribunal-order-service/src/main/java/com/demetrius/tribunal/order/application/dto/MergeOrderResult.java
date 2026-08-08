package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.domain.model.MergeOrder;
import com.demetrius.tribunal.order.domain.model.MergeOrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合单应用层出参。
 */
public record MergeOrderResult(
        String id,
        String mergeNo,
        String customerId,
        List<String> memberOrderIds,
        String status,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        String trackingNo,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<ItemResult> items) {

    public record ItemResult(
            String orderId,
            String orderNo,
            String skuCode,
            String skuName,
            BigDecimal quantity,
            BigDecimal unitAmount,
            BigDecimal subTotal) {
    }

    public static MergeOrderResult from(MergeOrder mergeOrder) {
        List<ItemResult> itemResults = mergeOrder.getItems().stream()
                .map(i -> new ItemResult(i.orderId(), i.orderNo(), i.skuCode(), i.skuName(),
                        i.quantity(), i.unitAmount(), i.subTotal()))
                .toList();
        return new MergeOrderResult(
                mergeOrder.getId(),
                mergeOrder.getMergeNo(),
                mergeOrder.getCustomerId(),
                mergeOrder.getMemberOrderIds(),
                mergeOrder.getStatus().name(),
                mergeOrder.getTotalAmount(),
                mergeOrder.getShippingFee(),
                mergeOrder.getTrackingNo(),
                mergeOrder.getCreateTime(),
                mergeOrder.getUpdateTime(),
                itemResults);
    }
}
