package com.demetrius.tribunal.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.order.domain.model.AfterSale;
import com.demetrius.tribunal.order.domain.model.AfterSaleItem;
import com.demetrius.tribunal.order.domain.model.AfterSaleReason;
import com.demetrius.tribunal.order.domain.model.AfterSaleType;
import com.demetrius.tribunal.order.domain.repository.AfterSaleRepository;
import com.demetrius.tribunal.order.infrastructure.mapper.AfterSaleItemMapper;
import com.demetrius.tribunal.order.infrastructure.mapper.AfterSaleMapper;
import com.demetrius.tribunal.order.infrastructure.model.AfterSaleItemPo;
import com.demetrius.tribunal.order.infrastructure.model.AfterSalePo;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 售后单仓储实现：PO 与领域聚合互转。
 */
@Repository
public class AfterSaleRepositoryImpl implements AfterSaleRepository {

    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleItemMapper afterSaleItemMapper;

    public AfterSaleRepositoryImpl(AfterSaleMapper afterSaleMapper,
                                   AfterSaleItemMapper afterSaleItemMapper) {
        this.afterSaleMapper = afterSaleMapper;
        this.afterSaleItemMapper = afterSaleItemMapper;
    }

    @Override
    public void save(AfterSale afterSale) {
        AfterSalePo po = toPo(afterSale);
        // upsert：先查是否存在
        AfterSalePo existing = afterSaleMapper.selectById(po.getId());
        if (existing == null) {
            afterSaleMapper.insert(po);
        } else {
            afterSaleMapper.updateById(po);
        }

        // 明细：删除旧的 + 插入新的（简化处理，售后单创建后明细不变）
        List<AfterSaleItemPo> existingItems = afterSaleItemMapper.findByAfterSaleId(afterSale.getId());
        for (AfterSaleItemPo item : existingItems) {
            afterSaleItemMapper.deleteById(item.getId());
        }
        for (AfterSaleItemPo itemPo : toItemPos(afterSale)) {
            afterSaleItemMapper.insert(itemPo);
        }
    }

    @Override
    public Optional<AfterSale> findById(String id) {
        AfterSalePo po = afterSaleMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<AfterSale> findByAfterSaleNo(String afterSaleNo) {
        AfterSalePo po = afterSaleMapper.selectOne(
                new LambdaQueryWrapper<AfterSalePo>()
                        .eq(AfterSalePo::getAfterSaleNo, afterSaleNo));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<AfterSale> findByOrderId(String orderId) {
        List<AfterSalePo> pos = afterSaleMapper.selectList(
                new LambdaQueryWrapper<AfterSalePo>()
                        .eq(AfterSalePo::getOrderId, orderId)
                        .orderByDesc(AfterSalePo::getCreateTime));
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AfterSale> findByCustomerId(String customerId) {
        List<AfterSalePo> pos = afterSaleMapper.selectList(
                new LambdaQueryWrapper<AfterSalePo>()
                        .eq(AfterSalePo::getCustomerId, customerId)
                        .orderByDesc(AfterSalePo::getCreateTime));
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    // ---------- 转换 ----------

    private AfterSalePo toPo(AfterSale afterSale) {
        AfterSalePo po = new AfterSalePo();
        po.setId(afterSale.getId());
        po.setAfterSaleNo(afterSale.getAfterSaleNo());
        po.setOrderId(afterSale.getOrderId());
        po.setOrderNo(afterSale.getOrderNo());
        po.setCustomerId(afterSale.getCustomerId());
        po.setType(afterSale.getType().name());
        po.setReason(afterSale.getReason().name());
        po.setStatus(afterSale.getStatus().name());
        po.setTotalRefundAmount(afterSale.getTotalRefundAmount());
        po.setRejectReason(afterSale.getRejectReason());
        po.setRefundTxnNo(afterSale.getRefundTxnNo());
        po.setCreateTime(afterSale.getCreateTime());
        po.setUpdateTime(afterSale.getUpdateTime());
        return po;
    }

    private List<AfterSaleItemPo> toItemPos(AfterSale afterSale) {
        List<AfterSaleItemPo> result = new ArrayList<>();
        for (AfterSaleItem item : afterSale.getItems()) {
            AfterSaleItemPo po = new AfterSaleItemPo();
            po.setAfterSaleId(afterSale.getId());
            po.setSkuCode(item.skuCode());
            po.setSkuName(item.skuName());
            po.setQuantity(item.quantity());
            po.setRefundAmount(item.refundAmount());
            po.setDepositRefund(item.depositRefund());
            po.setCreateTime(afterSale.getCreateTime());
            result.add(po);
        }
        return result;
    }

    private AfterSale toDomain(AfterSalePo po) {
        List<AfterSaleItemPo> itemPos = afterSaleItemMapper.findByAfterSaleId(po.getId());
        List<AfterSaleItem> items = itemPos.stream()
                .map(ip -> new AfterSaleItem(
                        ip.getSkuCode(), ip.getSkuName(), ip.getQuantity(),
                        ip.getRefundAmount(), ip.getDepositRefund()))
                .collect(Collectors.toList());

        return AfterSale.restore(
                po.getId(), po.getAfterSaleNo(), po.getOrderId(), po.getOrderNo(),
                po.getCustomerId(),
                AfterSaleType.valueOf(po.getType()),
                AfterSaleReason.valueOf(po.getReason()),
                items,
                com.demetrius.tribunal.order.domain.model.AfterSaleStatus.valueOf(po.getStatus()),
                po.getTotalRefundAmount(), po.getRejectReason(), po.getRefundTxnNo(),
                po.getCreateTime(), po.getUpdateTime());
    }
}
