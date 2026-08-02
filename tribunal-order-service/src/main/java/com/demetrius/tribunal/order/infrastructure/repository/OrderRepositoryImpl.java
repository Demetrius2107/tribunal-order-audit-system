package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.OrderStatus;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderSkuMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderPo;
import com.demetrius.tribunal.order.infrastructure.model.OrderSkuPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单仓储实现（infrastructure 层，MyBatis-Plus）。
 *
 * <p>职责：领域聚合 ↔ 持久化对象（PO）的转换 + 落库。</p>
 * <p>注意：仓储只做「存取」，不做业务校验——业务规则在领域层。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>① 幂等判断：save 时按 orderNo 查重（对照旧项目订单编号唯一约束）</li>
 *   <li>② 事务边界：save 需要同时写 order + order_sku，事务应放在应用服务层（当前 @Transactional 在应用服务）</li>
 *   <li>③ 乐观锁：PO 加 @Version，save 时防止并发覆盖（对照旧项目 ReentrantLock 想解决的问题）</li>
 *   <li>④ 状态流水：每次状态变更写 t_order_status_record（对照旧项目 saveOrderStatusProcessRecordDomain）</li>
 * </ul>
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;

    private final OrderSkuMapper orderSkuMapper;

    public OrderRepositoryImpl(OrderMapper orderMapper, OrderSkuMapper orderSkuMapper) {
        this.orderMapper = orderMapper;
        this.orderSkuMapper = orderSkuMapper;
    }

    @Override
    @Transactional
    public void save(Order order) {
        OrderPo po = toPo(order);
        OrderPo exist = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderPo>().eq(OrderPo::getOrderNo, order.getOrderNo()));
        if (exist == null) {
            orderMapper.insert(po);
        } else {
            orderMapper.updateById(po);
        }
        // TODO（学习任务）：明细的增量/全量更新策略（简单做法：先删后插，注意保持事务）
        orderSkuMapper.delete(new LambdaQueryWrapper<OrderSkuPo>()
                .eq(OrderSkuPo::getOrderId, order.getId().value()));
        for (OrderSku sku : order.getSkus()) {
            OrderSkuPo skuPo = toSkuPo(order, sku);
            orderSkuMapper.insert(skuPo);
        }
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        OrderPo po = orderMapper.selectById(id.value());
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<Order> findByOrderNo(String orderNo) {
        OrderPo po = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderPo>().eq(OrderPo::getOrderNo, orderNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void delete(OrderId id) {
        // 逻辑删除（PO 有 @TableLogic 注解，deleteById 自动转 UPDATE）
        orderMapper.deleteById(id.value());
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private Order toDomain(OrderPo po) {
        List<OrderSkuPo> skuPos = orderSkuMapper.findByOrderId(po.getId());
        List<OrderSku> skus = skuPos.stream()
                .map(s -> new OrderSku(s.getSkuCode(), s.getSkuName(), s.getQuantity(), s.getPrice()))
                .toList();
        // 完整还原聚合：状态/金额/拒绝原因/时间戳均来自数据库（restore 不做任何重算）
        return Order.restore(
                new OrderId(po.getId()),
                po.getOrderNo(),
                po.getCustomerId(),
                skus,
                OrderStatus.valueOf(po.getStatus()),
                po.getTotalAmount(),
                po.getDiscountAmount(),
                po.getPayableAmount(),
                po.getRejectReason(),
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private OrderPo toPo(Order order) {
        OrderPo po = new OrderPo();
        po.setId(order.getId().value());
        po.setOrderNo(order.getOrderNo());
        po.setCustomerId(order.getCustomerId());
        po.setStatus(order.getStatus().name());
        po.setTotalAmount(order.getTotalAmount());
        po.setDiscountAmount(order.getDiscountAmount());
        po.setPayableAmount(order.getPayableAmount());
        po.setRejectReason(order.getRejectReason());
        po.setCreateTime(order.getCreateTime());
        po.setUpdateTime(order.getUpdateTime());
        return po;
    }

    private OrderSkuPo toSkuPo(Order order, OrderSku sku) {
        OrderSkuPo po = new OrderSkuPo();
        po.setOrderId(order.getId().value());
        po.setSkuCode(sku.getSkuCode());
        po.setSkuName(sku.getSkuName());
        po.setQuantity(sku.getQuantity());
        po.setPrice(sku.getPrice());
        po.setAmount(sku.getAmount());
        po.setCreateTime(LocalDateTime.now());
        return po;
    }
}
