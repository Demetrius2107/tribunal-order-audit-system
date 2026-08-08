package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderSku;
import com.demetrius.tribunal.order.domain.model.OrderStatus;
import com.demetrius.tribunal.order.domain.model.OrderType;
import com.demetrius.tribunal.order.domain.model.ReturnablePackaging;
import com.demetrius.tribunal.order.domain.repository.OrderPage;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.OrderSkuMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.ReturnablePackagingMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderPo;
import com.demetrius.tribunal.order.infrastructure.model.OrderSkuPo;
import com.demetrius.tribunal.order.infrastructure.model.ReturnablePackagingPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 订单仓储实现（infrastructure 层，MyBatis-Plus）。
 *
 * <p>职责：领域聚合 ↔ 持久化对象（PO）的转换 + 落库。</p>
 * <p>注意：仓储只做「存取」，不做业务校验——业务规则在领域层。</p>
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>① 幂等判断：save 时按 orderNo 查重（参照通用做法</li>
 *   <li>② 事务边界：save 需要同时写 order + order_sku，事务应放在应用服务层（当前 @Transactional 在应用服务）</li>
 *   <li>③ 乐观锁：PO 加 @Version，save 时防止并发覆盖（参照通用做法</li>
 *   <li>④ 状态流水：每次状态变更写 t_order_status_record（参照通用做法</li>
 * </ul>
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;

    private final OrderSkuMapper orderSkuMapper;

    private final ReturnablePackagingMapper returnablePackagingMapper;

    public OrderRepositoryImpl(OrderMapper orderMapper, OrderSkuMapper orderSkuMapper,
                               ReturnablePackagingMapper returnablePackagingMapper) {
        this.orderMapper = orderMapper;
        this.orderSkuMapper = orderSkuMapper;
        this.returnablePackagingMapper = returnablePackagingMapper;
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
        // 明细：先删后插（保持事务）
        orderSkuMapper.delete(new LambdaQueryWrapper<OrderSkuPo>()
                .eq(OrderSkuPo::getOrderId, order.getId().value()));
        for (OrderSku sku : order.getSkus()) {
            OrderSkuPo skuPo = toSkuPo(order, sku);
            orderSkuMapper.insert(skuPo);
        }
        // 空包装回收明细：先删后插
        returnablePackagingMapper.delete(new LambdaQueryWrapper<ReturnablePackagingPo>()
                .eq(ReturnablePackagingPo::getOrderId, order.getId().value()));
        for (ReturnablePackaging rp : order.getReturnablePackagings()) {
            ReturnablePackagingPo rpPo = toReturnablePo(order, rp);
            returnablePackagingMapper.insert(rpPo);
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
    public OrderPage findPage(String customerId, String status, long pageNum, long pageSize) {
        LambdaQueryWrapper<OrderPo> wrapper = new LambdaQueryWrapper<>();
        if (customerId != null && !customerId.isBlank()) {
            wrapper.eq(OrderPo::getCustomerId, customerId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(OrderPo::getStatus, status);
        }
        wrapper.orderByDesc(OrderPo::getCreateTime);

        Page<OrderPo> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Order> orders = page.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
        return OrderPage.of(page.getTotal(), pageNum, pageSize, orders);
    }

    @Override
    public void delete(OrderId id) {
        // 逻辑删除（PO 有 @TableLogic 注解，deleteById 自动转 UPDATE）
        orderMapper.deleteById(id.value());
    }

    @Override
    public List<Order> findByParentOrderId(String parentOrderId) {
        List<OrderPo> pos = orderMapper.selectList(
                new LambdaQueryWrapper<OrderPo>().eq(OrderPo::getParentOrderId, parentOrderId));
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private Order toDomain(OrderPo po) {
        List<OrderSkuPo> skuPos = orderSkuMapper.findByOrderId(po.getId());
        List<OrderSku> skus = skuPos.stream()
                .map(s -> {
                    OrderSku sku = new OrderSku(s.getSkuCode(), s.getSkuName(), s.getQuantity(), s.getPrice());
                    // M4：还原寻源仓库（未寻源时为 null）
                    if (s.getWarehouseId() != null && !s.getWarehouseId().isBlank()) {
                        sku.assignWarehouse(s.getWarehouseId());
                    }
                    return sku;
                })
                .toList();
        List<ReturnablePackagingPo> rpPos = returnablePackagingMapper.findByOrderId(po.getId());
        List<ReturnablePackaging> rps = rpPos.stream()
                .map(r -> new ReturnablePackaging(
                        r.getPackagingType(), r.getPackagingName(), r.getQuantity(), r.getUnitDeposit()))
                .toList();
        // 完整还原聚合：状态/金额/拒绝原因/时间戳均来自数据库（restore 不做任何重算）
        return Order.restore(
                new OrderId(po.getId()),
                po.getOrderNo(),
                po.getCustomerId(),
                OrderType.valueOf(po.getOrderType()),
                Boolean.TRUE.equals(po.getCarPooling()),
                Boolean.TRUE.equals(po.getCarPoolJoined()),
                skus,
                rps,
                OrderStatus.valueOf(po.getStatus()),
                po.getTotalAmount(),
                po.getDiscountAmount(),
                po.getDiscountPoolDeduction(),
                po.getDepositAmount(),
                po.getTaxAmount(),
                po.getShippingFee(),
                po.getPayableAmount(),
                po.getRejectReason(),
                po.getParentOrderId(),
                Boolean.TRUE.equals(po.getSplit()),
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private OrderPo toPo(Order order) {
        OrderPo po = new OrderPo();
        po.setId(order.getId().value());
        po.setOrderNo(order.getOrderNo());
        po.setCustomerId(order.getCustomerId());
        po.setOrderType(order.getOrderType().name());
        po.setCarPooling(order.isCarPooling());
        po.setCarPoolJoined(order.isCarPoolJoined());
        po.setStatus(order.getStatus().name());
        po.setParentOrderId(order.getParentOrderId());
        po.setSplit(order.isSplit());
        po.setTotalAmount(order.getTotalAmount());
        po.setDiscountAmount(order.getDiscountAmount());
        po.setDiscountPoolDeduction(order.getDiscountPoolDeduction());
        po.setDepositAmount(order.getDepositAmount());
        po.setTaxAmount(order.getTaxAmount());
        po.setShippingFee(order.getShippingFee());
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
        po.setWarehouseId(sku.getWarehouseId());
        po.setCreateTime(LocalDateTime.now());
        return po;
    }

    private ReturnablePackagingPo toReturnablePo(Order order, ReturnablePackaging rp) {
        ReturnablePackagingPo po = new ReturnablePackagingPo();
        po.setOrderId(order.getId().value());
        po.setPackagingType(rp.getPackagingType());
        po.setPackagingName(rp.getPackagingName());
        po.setQuantity(rp.getQuantity());
        po.setUnitDeposit(rp.getUnitDeposit());
        po.setDepositAmount(rp.getDepositAmount());
        return po;
    }
}
