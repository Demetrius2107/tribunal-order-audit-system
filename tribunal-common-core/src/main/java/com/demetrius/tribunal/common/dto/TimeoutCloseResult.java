package com.demetrius.tribunal.common.dto;

import java.util.List;

/**
 * 超时关单结果 DTO（跨服务 Feign 返回体：task-service 调度 → order-service 执行）。
 *
 * <p>用途：task-service 定时调用 order-service 的超时关单接口，
 * 返回关闭数量与关闭的订单编号，供 TaskLog 记录 processedCount。</p>
 */
public record TimeoutCloseResult(
        int closedCount,
        List<String> closedOrderNos) {

    public static TimeoutCloseResult of(int closedCount, List<String> closedOrderNos) {
        return new TimeoutCloseResult(closedCount, closedOrderNos);
    }
}
