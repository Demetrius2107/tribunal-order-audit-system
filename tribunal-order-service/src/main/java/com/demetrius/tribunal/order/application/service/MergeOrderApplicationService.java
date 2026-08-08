package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.application.dto.MergeOrderResult;
import com.demetrius.tribunal.order.domain.model.MergeOrder;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.repository.MergeOrderRepository;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 合单应用服务（用例编排层）。
 *
 * <p>编排合单全流程：创建 → 打包 → 发货 → 送达。</p>
 *
 * <p>核心规则：</p>
 * <ul>
 *   <li>至少 2 个订单才能合单</li>
 *   <li>成员订单必须属于同一客户</li>
 *   <li>成员订单状态须为已确认/已转单</li>
 *   <li>合单取消后成员订单恢复独立可发货状态</li>
 * </ul>
 */
@Service
public class MergeOrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MergeOrderApplicationService.class);

    private final MergeOrderRepository mergeOrderRepository;
    private final OrderRepository orderRepository;

    public MergeOrderApplicationService(MergeOrderRepository mergeOrderRepository,
                                         OrderRepository orderRepository) {
        this.mergeOrderRepository = mergeOrderRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * 创建合单。
     *
     * @param orderIds 成员订单 ID 列表
     * @return 合单结果
     */
    @Transactional
    public MergeOrderResult create(List<String> orderIds) {
        if (orderIds == null || orderIds.size() < 2) {
            throw new BizException("400101", "合单至少需要2个成员订单");
        }

        // 加载所有成员订单
        List<Order> memberOrders = new ArrayList<>();
        for (String orderId : orderIds) {
            Order order = orderRepository.findById(new OrderId(orderId))
                    .orElseThrow(() -> new BizException("404001", "订单不存在: " + orderId));
            memberOrders.add(order);
        }

        String id = UUID.randomUUID().toString().replace("-", "");
        String mergeNo = generateMergeNo();

        MergeOrder mergeOrder = MergeOrder.create(id, mergeNo, memberOrders);
        mergeOrderRepository.save(mergeOrder);

        log.info("合单已创建: mergeNo={}, memberOrders={}", mergeNo, orderIds);
        return MergeOrderResult.from(mergeOrder);
    }

    /** 打包：CREATED → PACKED */
    @Transactional
    public MergeOrderResult pack(String mergeOrderId) {
        MergeOrder mergeOrder = loadOrThrow(mergeOrderId);
        mergeOrder.pack();
        mergeOrderRepository.save(mergeOrder);
        log.info("合单已打包: mergeNo={}", mergeOrder.getMergeNo());
        return MergeOrderResult.from(mergeOrder);
    }

    /** 发货：PACKED → SHIPPED */
    @Transactional
    public MergeOrderResult ship(String mergeOrderId, String trackingNo) {
        MergeOrder mergeOrder = loadOrThrow(mergeOrderId);
        mergeOrder.ship(trackingNo);
        mergeOrderRepository.save(mergeOrder);
        log.info("合单已发货: mergeNo={}, trackingNo={}", mergeOrder.getMergeNo(), trackingNo);
        return MergeOrderResult.from(mergeOrder);
    }

    /** 送达：SHIPPED → DELIVERED */
    @Transactional
    public MergeOrderResult deliver(String mergeOrderId) {
        MergeOrder mergeOrder = loadOrThrow(mergeOrderId);
        mergeOrder.deliver();
        mergeOrderRepository.save(mergeOrder);
        log.info("合单已送达: mergeNo={}", mergeOrder.getMergeNo());
        return MergeOrderResult.from(mergeOrder);
    }

    /** 取消：CREATED → CANCELLED */
    @Transactional
    public MergeOrderResult cancel(String mergeOrderId) {
        MergeOrder mergeOrder = loadOrThrow(mergeOrderId);
        mergeOrder.cancel();
        mergeOrderRepository.save(mergeOrder);
        log.info("合单已取消: mergeNo={}", mergeOrder.getMergeNo());
        return MergeOrderResult.from(mergeOrder);
    }

    /** 设置合单运费（合并后重新计算的合并运费） */
    @Transactional
    public MergeOrderResult applyShippingFee(String mergeOrderId, java.math.BigDecimal fee) {
        MergeOrder mergeOrder = loadOrThrow(mergeOrderId);
        mergeOrder.applyShippingFee(fee);
        mergeOrderRepository.save(mergeOrder);
        return MergeOrderResult.from(mergeOrder);
    }

    /** 查询合单详情 */
    @Transactional(readOnly = true)
    public MergeOrderResult getById(String mergeOrderId) {
        return MergeOrderResult.from(loadOrThrow(mergeOrderId));
    }

    /** 查询客户的所有合单 */
    @Transactional(readOnly = true)
    public List<MergeOrderResult> listByCustomer(String customerId) {
        return mergeOrderRepository.findByCustomerId(customerId).stream()
                .map(MergeOrderResult::from)
                .toList();
    }

    /** 查询某订单参与的合单 */
    @Transactional(readOnly = true)
    public MergeOrderResult getByMemberOrder(String orderId) {
        return mergeOrderRepository.findByMemberOrderId(orderId)
                .map(MergeOrderResult::from)
                .orElseThrow(() -> new BizException("404102", "该订单未参与任何合单: " + orderId));
    }

    // ---------- 内部方法 ----------

    private MergeOrder loadOrThrow(String mergeOrderId) {
        return mergeOrderRepository.findById(mergeOrderId)
                .orElseThrow(() -> new BizException("404101", "合单不存在: " + mergeOrderId));
    }

    private String generateMergeNo() {
        return "MG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
