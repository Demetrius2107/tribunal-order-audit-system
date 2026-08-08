package com.demetrius.tribunal.order.interfaces.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 创建合单请求（接口层 DTO）。
 *
 * @param orderIds 成员订单 ID 列表（至少 2 个，须属于同一客户）
 */
public record MergeOrderCreateRequest(
        @NotEmpty List<String> orderIds) {
}
