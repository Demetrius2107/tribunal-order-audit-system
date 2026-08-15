package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.domain.model.PreOrderActivity;
import com.demetrius.tribunal.order.domain.model.PreOrderActivityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预购活动应用层出参（F-312）。
 */
public record PreOrderActivityResult(
        String activityId,
        String activityNo,
        String name,
        List<String> skuCodes,
        BigDecimal depositRate,
        BigDecimal discountRate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        PreOrderActivityStatus status,
        LocalDateTime createTime) {

    /** 聚合 → 应用层 DTO。 */
    public static PreOrderActivityResult from(PreOrderActivity activity) {
        return new PreOrderActivityResult(
                activity.getId(),
                activity.getActivityNo(),
                activity.getName(),
                activity.getSkuCodes(),
                activity.getDepositRate(),
                activity.getDiscountRate(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getStatus(),
                activity.getCreateTime());
    }
}
