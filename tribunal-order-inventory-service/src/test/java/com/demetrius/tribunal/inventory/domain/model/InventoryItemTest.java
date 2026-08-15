package com.demetrius.tribunal.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 库存领域超卖防线单测（并发超卖防护第一层：可售量校验）。
 */
class InventoryItemTest {

    private InventoryItem item(int total, int reserved) {
        return InventoryItem.restore(
                new InventoryItemId("inv-001"), "SKU001", "测试商品", "件",
                new BigDecimal(total), new BigDecimal(reserved), 0);
    }

    @Test
    @DisplayName("预占：可售量充足时成功")
    void reserveWithinAvailable() {
        InventoryItem item = item(100, 0);
        item.reserve(new BigDecimal("40"));
        assertEquals(0, new BigDecimal("40").compareTo(item.getReservedQuantity()));
        assertEquals(0, new BigDecimal("60").compareTo(item.availableQuantity()));
    }

    @Test
    @DisplayName("预占：超过可售量被拒（防超卖第一层防线）")
    void reserveOverAvailableRejected() {
        InventoryItem item = item(100, 70); // 可售 30
        assertThrows(IllegalStateException.class, () -> item.reserve(new BigDecimal("31")));
        assertEquals(0, new BigDecimal("70").compareTo(item.getReservedQuantity()),
                "预占失败不应改变已预占数量");
    }

    @Test
    @DisplayName("预占：并发下可售量临界（恰好用完）允许")
    void reserveExactlyAvailable() {
        InventoryItem item = item(100, 0);
        item.reserve(new BigDecimal("100"));
        assertEquals(0, new BigDecimal("100").compareTo(item.getReservedQuantity()));
    }

    @Test
    @DisplayName("释放：超过已预占数量被拒")
    void releaseOverReservedRejected() {
        InventoryItem item = item(100, 30);
        assertThrows(IllegalStateException.class, () -> item.release(new BigDecimal("31")));
    }

    @Test
    @DisplayName("restore：版本号随库存还原")
    void restoreKeepsVersion() {
        InventoryItem item = InventoryItem.restore(
                new InventoryItemId("inv-001"), "SKU001", "测试商品", "件",
                new BigDecimal("100"), new BigDecimal("20"), 7);
        assertEquals(7, item.getVersion());
    }
}
