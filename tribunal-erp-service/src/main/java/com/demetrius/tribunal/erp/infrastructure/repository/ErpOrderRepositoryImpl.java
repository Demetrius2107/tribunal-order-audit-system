package com.demetrius.tribunal.erp.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.erp.domain.model.ErpOrder;
import com.demetrius.tribunal.erp.domain.model.ErpOrderId;
import com.demetrius.tribunal.erp.domain.model.ErpOrderLine;
import com.demetrius.tribunal.erp.domain.model.ErpOrderStatus;
import com.demetrius.tribunal.erp.domain.repository.ErpOrderRepository;
import com.demetrius.tribunal.erp.infrastructure.mapper.ErpOrderLineMapper;
import com.demetrius.tribunal.erp.infrastructure.mapper.ErpOrderMapper;
import com.demetrius.tribunal.erp.infrastructure.model.ErpOrderLinePo;
import com.demetrius.tribunal.erp.infrastructure.model.ErpOrderPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ERP 履约订单仓储实现（MyBatis-Plus）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>接收转单幂等：save 前按 sourceOrderNo 查重（重复转单拒绝）</li>
 *   <li>明细更新策略：先删后插保持事务</li>
 *   <li>乐观锁：PO 加 @Version 防止并发覆盖</li>
 * </ul>
 */
@Repository
public class ErpOrderRepositoryImpl implements ErpOrderRepository {

    private final ErpOrderMapper erpOrderMapper;

    private final ErpOrderLineMapper erpOrderLineMapper;

    public ErpOrderRepositoryImpl(ErpOrderMapper erpOrderMapper, ErpOrderLineMapper erpOrderLineMapper) {
        this.erpOrderMapper = erpOrderMapper;
        this.erpOrderLineMapper = erpOrderLineMapper;
    }

    @Override
    @Transactional
    public void save(ErpOrder order) {
        ErpOrderPo po = toPo(order);
        ErpOrderPo exist = erpOrderMapper.selectOne(
                new LambdaQueryWrapper<ErpOrderPo>().eq(ErpOrderPo::getSourceOrderNo, order.getSourceOrderNo()));
        if (exist == null) {
            erpOrderMapper.insert(po);
        } else {
            erpOrderMapper.updateById(po);
        }
        erpOrderLineMapper.delete(new LambdaQueryWrapper<ErpOrderLinePo>()
                .eq(ErpOrderLinePo::getErpOrderId, order.getId().value()));
        for (ErpOrderLine line : order.getLines()) {
            erpOrderLineMapper.insert(toLinePo(order, line));
        }
    }

    @Override
    public Optional<ErpOrder> findById(ErpOrderId id) {
        ErpOrderPo po = erpOrderMapper.selectById(id.value());
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<ErpOrder> findBySourceOrderNo(String sourceOrderNo) {
        ErpOrderPo po = erpOrderMapper.selectOne(
                new LambdaQueryWrapper<ErpOrderPo>().eq(ErpOrderPo::getSourceOrderNo, sourceOrderNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void delete(ErpOrderId id) {
        erpOrderMapper.deleteById(id.value());
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private ErpOrder toDomain(ErpOrderPo po) {
        List<ErpOrderLinePo> linePos = erpOrderLineMapper.findByErpOrderId(po.getId());
        List<ErpOrderLine> lines = linePos.stream()
                .map(l -> new ErpOrderLine(l.getSkuCode(), l.getSkuName(), l.getQuantity(), l.getPrice()))
                .toList();
        ErpOrder order = ErpOrder.receive(
                new ErpOrderId(po.getId()), po.getSourceOrderNo(), po.getCustomerId(), lines);
        // TODO（学习任务）：还原履约状态/时间戳（对照 Order.restore 思路，后续补充）
        return order;
    }

    private ErpOrderPo toPo(ErpOrder order) {
        ErpOrderPo po = new ErpOrderPo();
        po.setId(order.getId().value());
        po.setSourceOrderNo(order.getSourceOrderNo());
        po.setCustomerId(order.getCustomerId());
        po.setStatus(order.getStatus().name());
        po.setTotalAmount(order.getTotalAmount());
        po.setReceivedAt(order.getReceivedAt());
        po.setShippedAt(order.getShippedAt());
        po.setSignedAt(order.getSignedAt());
        po.setUpdateTime(order.getUpdateTime());
        return po;
    }

    private ErpOrderLinePo toLinePo(ErpOrder order, ErpOrderLine line) {
        ErpOrderLinePo po = new ErpOrderLinePo();
        po.setErpOrderId(order.getId().value());
        po.setSkuCode(line.getSkuCode());
        po.setSkuName(line.getSkuName());
        po.setQuantity(line.getQuantity());
        po.setPrice(line.getPrice());
        po.setAmount(line.getAmount());
        po.setCreateTime(LocalDateTime.now());
        return po;
    }
}
