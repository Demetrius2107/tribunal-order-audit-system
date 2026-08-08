package com.demetrius.tribunal.order.domain.model;

import java.util.List;

/**
 * M4：拆单结果值对象。
 *
 * @param parent   父单（已标记 SPLITTED / split=true）
 * @param children 拆出的子单列表（每仓一张）
 */
public record SplitResult(Order parent, List<Order> children) {
}
