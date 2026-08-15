package com.demetrius.tribunal.inventory.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.inventory.domain.model.InventoryItem;
import com.demetrius.tribunal.inventory.domain.model.InventoryItemId;
import com.demetrius.tribunal.inventory.domain.repository.InventoryItemRepository;
import com.demetrius.tribunal.inventory.infrastructure.mapper.InventoryFlowMapper;
import com.demetrius.tribunal.inventory.infrastructure.model.InventoryFlowPo;
import com.demetrius.tribunal.inventory.infrastructure.repository.InventoryItemRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 并发超卖防护应用层单测（乐观锁冲突重试 + 多线程预占不超卖）。
 */
class InventoryApplicationServiceConcurrencyTest {

    /** 内存版库存仓储：模拟数据库乐观锁（version CAS），供多线程真实并发测试 */
    private static class InMemoryInventoryRepository implements InventoryItemRepository {

        private final java.util.concurrent.ConcurrentHashMap<String, InventoryItem> store =
                new java.util.concurrent.ConcurrentHashMap<>();

        void seed(InventoryItem item) {
            store.put(item.getSkuCode(), item);
        }

        @Override
        public void save(InventoryItem item) {
            // CAS：版本不匹配则冲突（模拟 updateById WHERE version=? 影响行数 0）
            store.compute(item.getSkuCode(), (sku, current) -> {
                if (current != null && !current.getVersion().equals(item.getVersion())) {
                    throw new InventoryItemRepositoryImpl.OptimisticLockConflictException(sku);
                }
                InventoryItem updated = InventoryItem.restore(
                        item.getId(), item.getSkuCode(), item.getSkuName(), item.getUnit(),
                        item.getTotalQuantity(), item.getReservedQuantity(),
                        item.getVersion() + 1);
                return updated;
            });
        }

        @Override
        public java.util.Optional<InventoryItem> findById(String id) {
            return store.values().stream()
                    .filter(i -> i.getId().value().equals(id))
                    .findFirst();
        }

        @Override
        public java.util.Optional<InventoryItem> findBySkuCode(String skuCode) {
            InventoryItem current = store.get(skuCode);
            return current == null ? java.util.Optional.empty() : java.util.Optional.of(copyOf(current));
        }

        /** 深拷贝：模拟 DB select 返回快照，避免应用层就地修改污染 store。 */
        private InventoryItem copyOf(InventoryItem item) {
            return InventoryItem.restore(
                    item.getId(), item.getSkuCode(), item.getSkuName(), item.getUnit(),
                    item.getTotalQuantity(), item.getReservedQuantity(), item.getVersion());
        }

        @Override
        public void delete(String id) {
            store.values().removeIf(i -> i.getId().value().equals(id));
        }
    }

    private InventoryFlowMapper flowMapper;

    @BeforeEach
    void setUp() {
        flowMapper = mock(InventoryFlowMapper.class);
        // insert 返回 int，mock 默认返回 0 即可；无需 stub
    }

    private InventoryItem seedItem(int total) {
        return InventoryItem.restore(
                new InventoryItemId("inv-001"), "SKU001", "测试商品", "件",
                new BigDecimal(total), BigDecimal.ZERO, 0);
    }

    @Test
    @DisplayName("乐观锁冲突：首次写回冲突 → 重读重试成功")
    void optimisticLockConflictRetrySucceeds() {
        InventoryItemRepository repo = mock(InventoryItemRepository.class);
        InventoryItem v0 = seedItem(100);

        // 每次读返回新的快照（version=0、reserved=0），避免 mock 复用同一对象被就地修改
        when(repo.findBySkuCode("SKU001")).thenAnswer(inv -> java.util.Optional.of(seedItem(100)));
        doThrow(new InventoryItemRepositoryImpl.OptimisticLockConflictException("SKU001"))
                .doNothing()
                .when(repo).save(any(InventoryItem.class));

        InventoryApplicationService service =
                new InventoryApplicationService(repo, flowMapper);

        InventoryItem result = service.reserve("SKU001", new BigDecimal("10"));

        assertEquals(0, new BigDecimal("10").compareTo(result.getReservedQuantity()),
                "重试后预占成功");
        verify(repo, times(2)).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("乐观锁冲突：重试超限抛业务异常（不超卖）")
    void optimisticLockConflictExhaustedThrows() {
        InventoryItemRepository repo = mock(InventoryItemRepository.class);
        InventoryItem v0 = seedItem(100);
        when(repo.findBySkuCode("SKU001")).thenReturn(java.util.Optional.of(v0));
        doThrow(new InventoryItemRepositoryImpl.OptimisticLockConflictException("SKU001"))
                .when(repo).save(any(InventoryItem.class));

        InventoryApplicationService service =
                new InventoryApplicationService(repo, flowMapper);

        BizException ex = assertThrows(BizException.class,
                () -> service.reserve("SKU001", new BigDecimal("10")));
        assertEquals("400002", ex.getCode(), "应抛库存并发冲突业务码 400002");
        verify(repo, times(3)).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("多线程并发预占：总预占不超过总库存（不超卖）")
    void concurrentReserveNeverOverSells() throws InterruptedException {
        int total = 100;
        int threads = 20;
        int perThread = 6; // 20×6=120 > 100，必然有人失败
        InMemoryInventoryRepository repo = new InMemoryInventoryRepository();
        repo.seed(seedItem(total));

        InventoryApplicationService service =
                new InventoryApplicationService(repo, flowMapper);

        int success = runConcurrent(threads, () -> {
            try {
                service.reserve("SKU001", new BigDecimal(perThread));
                return true;
            } catch (Exception e) {
                return false; // 可售不足/冲突重试超限 → 该线程预占失败
            }
        });

        InventoryItem finalState = repo.findBySkuCode("SKU001").orElseThrow();
        BigDecimal reserved = finalState.getReservedQuantity();

        assertTrue(reserved.compareTo(new BigDecimal(total)) <= 0,
                "已预占 " + reserved + " 不得超过总库存 " + total + "（不超卖）");
        assertEquals(0, reserved.remainder(new BigDecimal(perThread)).compareTo(BigDecimal.ZERO),
                "成功预占数量应为 perThread 的整数倍");
        assertEquals(success * perThread, reserved.intValue(),
                "成功线程数 × 每线程预占 = 最终已预占（无丢失更新）");
    }

    private int runConcurrent(int threads, java.util.function.Supplier<Boolean> action)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 所有线程就绪后同时开跑
                    if (action.get()) {
                        success.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS), "线程未全部就绪");
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "并发预占未在超时内完成");
        pool.shutdownNow();
        return success.get();
    }
}
