package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.domain.model.MergeOrder;
import com.demetrius.tribunal.order.domain.model.MergeOrderItem;
import com.demetrius.tribunal.order.domain.model.MergeOrderStatus;
import com.demetrius.tribunal.order.domain.repository.MergeOrderRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.MergeOrderItemMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.MergeOrderMapper;
import com.demetrius.tribunal.order.infrastructure.model.MergeOrderItemPo;
import com.demetrius.tribunal.order.infrastructure.model.MergeOrderPo;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 合单仓储实现：PO 与领域聚合互转。
 *
 * <p>成员订单 ID 列表从明细表的 orderId 去重派生，不单独建关联表。</p>
 */
@Repository
public class MergeOrderRepositoryImpl implements MergeOrderRepository {

    private final MergeOrderMapper mergeOrderMapper;
    private final MergeOrderItemMapper mergeOrderItemMapper;

    public MergeOrderRepositoryImpl(MergeOrderMapper mergeOrderMapper,
                                     MergeOrderItemMapper mergeOrderItemMapper) {
        this.mergeOrderMapper = mergeOrderMapper;
        this.mergeOrderItemMapper = mergeOrderItemMapper;
    }

    @Override
    public void save(MergeOrder mergeOrder) {
        MergeOrderPo po = toPo(mergeOrder);
        // upsert 主表
        MergeOrderPo existing = mergeOrderMapper.selectById(po.getId());
        if (existing == null) {
            mergeOrderMapper.insert(po);
        } else {
            mergeOrderMapper.updateById(po);
        }

        // 明细：删除旧的 + 插入新的
        List<MergeOrderItemPo> existingItems = mergeOrderItemMapper.findByMergeOrderId(mergeOrder.getId());
        for (MergeOrderItemPo item : existingItems) {
            mergeOrderItemMapper.deleteById(item.getId());
        }
        for (MergeOrderItemPo itemPo : toItemPos(mergeOrder)) {
            mergeOrderItemMapper.insert(itemPo);
        }
    }

    @Override
    public Optional<MergeOrder> findById(String id) {
        MergeOrderPo po = mergeOrderMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<MergeOrder> findByMergeNo(String mergeNo) {
        MergeOrderPo po = mergeOrderMapper.selectOne(
                new LambdaQueryWrapper<MergeOrderPo>()
                        .eq(MergeOrderPo::getMergeNo, mergeNo));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<MergeOrder> findByCustomerId(String customerId) {
        List<MergeOrderPo> pos = mergeOrderMapper.selectList(
                new LambdaQueryWrapper<MergeOrderPo>()
                        .eq(MergeOrderPo::getCustomerId, customerId)
                        .orderByDesc(MergeOrderPo::getCreateTime));
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<MergeOrder> findByMemberOrderId(String orderId) {
        // 先从明细表查找该订单参与的合单 ID
        List<MergeOrderItemPo> itemPos = mergeOrderItemMapper.selectList(
                new LambdaQueryWrapper<MergeOrderItemPo>()
                        .eq(MergeOrderItemPo::getOrderId, orderId));
        if (itemPos.isEmpty()) {
            return Optional.empty();
        }
        String mergeOrderId = itemPos.get(0).getMergeOrderId();
        return findById(mergeOrderId);
    }

    // ---------- 转换 ----------

    private MergeOrderPo toPo(MergeOrder mergeOrder) {
        MergeOrderPo po = new MergeOrderPo();
        po.setId(mergeOrder.getId());
        po.setMergeNo(mergeOrder.getMergeNo());
        po.setCustomerId(mergeOrder.getCustomerId());
        po.setStatus(mergeOrder.getStatus().name());
        po.setShippingFee(mergeOrder.getShippingFee());
        po.setTrackingNo(mergeOrder.getTrackingNo());
        po.setCreateTime(mergeOrder.getCreateTime());
        po.setUpdateTime(mergeOrder.getUpdateTime());
        return po;
    }

    private List<MergeOrderItemPo> toItemPos(MergeOrder mergeOrder) {
        List<MergeOrderItemPo> result = new ArrayList<>();
        for (MergeOrderItem item : mergeOrder.getItems()) {
            MergeOrderItemPo po = new MergeOrderItemPo();
            po.setMergeOrderId(mergeOrder.getId());
            po.setOrderId(item.orderId());
            po.setOrderNo(item.orderNo());
            po.setSkuCode(item.skuCode());
            po.setSkuName(item.skuName());
            po.setQuantity(item.quantity());
            po.setUnitAmount(item.unitAmount());
            po.setCreateTime(mergeOrder.getCreateTime());
            result.add(po);
        }
        return result;
    }

    private MergeOrder toDomain(MergeOrderPo po) {
        List<MergeOrderItemPo> itemPos = mergeOrderItemMapper.findByMergeOrderId(po.getId());

        // 明细 → 领域值对象
        List<MergeOrderItem> items = itemPos.stream()
                .map(ip -> new MergeOrderItem(
                        ip.getOrderId(), ip.getOrderNo(),
                        ip.getSkuCode(), ip.getSkuName(),
                        ip.getQuantity(), ip.getUnitAmount()))
                .collect(Collectors.toList());

        // 成员订单 ID 从明细去重派生
        List<String> memberOrderIds = itemPos.stream()
                .map(MergeOrderItemPo::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        return MergeOrder.restore(
                po.getId(), po.getMergeNo(), po.getCustomerId(),
                memberOrderIds, items,
                MergeOrderStatus.valueOf(po.getStatus()),
                po.getShippingFee(), po.getTrackingNo(),
                po.getCreateTime(), po.getUpdateTime());
    }
}
