package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.financesettlement.domain.model.SettlementOrder;
import com.demetrius.tribunal.financesettlement.domain.repository.SettlementOrderRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.SettlementOrderMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.SettlementOrderPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 结算单仓储实现（infrastructure 层，PO ↔ Domain 转换）。
 */
@Repository
public class SettlementOrderRepositoryImpl implements SettlementOrderRepository {

    private final SettlementOrderMapper settlementOrderMapper;

    public SettlementOrderRepositoryImpl(SettlementOrderMapper settlementOrderMapper) {
        this.settlementOrderMapper = settlementOrderMapper;
    }

    @Override
    public Optional<SettlementOrder> findBySettlementId(String settlementId) {
        SettlementOrderPo po = settlementOrderMapper.selectOne(
                new LambdaQueryWrapper<SettlementOrderPo>()
                        .eq(SettlementOrderPo::getSettlementId, settlementId));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<SettlementOrder> findByOrderId(String orderId) {
        SettlementOrderPo po = settlementOrderMapper.selectOne(
                new LambdaQueryWrapper<SettlementOrderPo>()
                        .eq(SettlementOrderPo::getOrderId, orderId));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(SettlementOrder order) {
        SettlementOrderPo po = toPo(order);
        if (settlementOrderMapper.selectById(po.getId()) == null) {
            settlementOrderMapper.insert(po);
        } else {
            settlementOrderMapper.updateById(po);
        }
    }

    private SettlementOrder toDomain(SettlementOrderPo po) {
        return new SettlementOrder(
                po.getId(), po.getSettlementId(), po.getOrderId(), po.getUserId(), po.getMerchantId(),
                po.getStatus(), po.getTotalAmount(), po.getDiscountAmount(), po.getShippingFee(),
                po.getTaxAmount(), po.getPlatformFee(), po.getPaymentFee(), po.getNetAmount(),
                po.getPaymentMethod(), po.getPaymentCurrency(), po.getChannelTransactionId());
    }

    private SettlementOrderPo toPo(SettlementOrder order) {
        SettlementOrderPo po = new SettlementOrderPo();
        po.setId(order.getId());
        po.setSettlementId(order.getSettlementId());
        po.setOrderId(order.getOrderId());
        po.setUserId(order.getUserId());
        po.setMerchantId(order.getMerchantId());
        po.setStatus(order.getStatus());
        po.setTotalAmount(order.getTotalAmount());
        po.setDiscountAmount(order.getDiscountAmount());
        po.setShippingFee(order.getShippingFee());
        po.setTaxAmount(order.getTaxAmount());
        po.setPlatformFee(order.getPlatformFee());
        po.setPaymentFee(order.getPaymentFee());
        po.setNetAmount(order.getNetAmount());
        po.setPaymentMethod(order.getPaymentMethod());
        po.setPaymentCurrency(order.getPaymentCurrency());
        po.setChannelTransactionId(order.getChannelTransactionId());
        return po;
    }
}
