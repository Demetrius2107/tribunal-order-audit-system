package com.demetrius.tribunal.fulfillment.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentId;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentLine;
import com.demetrius.tribunal.fulfillment.domain.model.FulfillmentOrder;
import com.demetrius.tribunal.fulfillment.domain.repository.FulfillmentRepository;
import com.demetrius.tribunal.fulfillment.infrastructure.mapper.FulfillmentLineMapper;
import com.demetrius.tribunal.fulfillment.infrastructure.mapper.FulfillmentOrderMapper;
import com.demetrius.tribunal.fulfillment.infrastructure.model.FulfillmentLinePo;
import com.demetrius.tribunal.fulfillment.infrastructure.model.FulfillmentOrderPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 履约单仓储实现（MyBatis-Plus）。
 */
@Repository
public class FulfillmentRepositoryImpl implements FulfillmentRepository {

    private final FulfillmentOrderMapper fulfillmentOrderMapper;

    private final FulfillmentLineMapper fulfillmentLineMapper;

    public FulfillmentRepositoryImpl(FulfillmentOrderMapper fulfillmentOrderMapper,
                                     FulfillmentLineMapper fulfillmentLineMapper) {
        this.fulfillmentOrderMapper = fulfillmentOrderMapper;
        this.fulfillmentLineMapper = fulfillmentLineMapper;
    }

    @Override
    @Transactional
    public void save(FulfillmentOrder order) {
        FulfillmentOrderPo po = toPo(order);
        FulfillmentOrderPo exist = fulfillmentOrderMapper.selectOne(
                new LambdaQueryWrapper<FulfillmentOrderPo>()
                        .eq(FulfillmentOrderPo::getSourceOrderNo, order.getSourceOrderNo()));
        if (exist == null) {
            fulfillmentOrderMapper.insert(po);
        } else {
            fulfillmentOrderMapper.updateById(po);
        }
        fulfillmentLineMapper.delete(new LambdaQueryWrapper<FulfillmentLinePo>()
                .eq(FulfillmentLinePo::getFulfillmentId, order.getId().value()));
        for (FulfillmentLine line : order.getLines()) {
            fulfillmentLineMapper.insert(toLinePo(order, line));
        }
    }

    @Override
    public Optional<FulfillmentOrder> findById(FulfillmentId id) {
        FulfillmentOrderPo po = fulfillmentOrderMapper.selectById(id.value());
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<FulfillmentOrder> findBySourceOrderNo(String sourceOrderNo) {
        FulfillmentOrderPo po = fulfillmentOrderMapper.selectOne(
                new LambdaQueryWrapper<FulfillmentOrderPo>()
                        .eq(FulfillmentOrderPo::getSourceOrderNo, sourceOrderNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private FulfillmentOrder toDomain(FulfillmentOrderPo po) {
        List<FulfillmentLinePo> linePos = fulfillmentLineMapper.findByFulfillmentId(po.getId());
        List<FulfillmentLine> lines = linePos.stream()
                .map(l -> new FulfillmentLine(l.getSkuCode(), l.getSkuName(), l.getQuantity(), l.getPrice()))
                .toList();
        FulfillmentOrder order = FulfillmentOrder.create(
                new FulfillmentId(po.getId()), po.getSourceOrderNo(), po.getCustomerId(), lines);
        // TODO（学习任务）：还原履约状态/时间戳（对照 restore 思路）
        return order;
    }

    private FulfillmentOrderPo toPo(FulfillmentOrder order) {
        FulfillmentOrderPo po = new FulfillmentOrderPo();
        po.setId(order.getId().value());
        po.setSourceOrderNo(order.getSourceOrderNo());
        po.setCustomerId(order.getCustomerId());
        po.setStatus(order.getStatus().name());
        po.setTotalAmount(order.getTotalAmount());
        po.setCreatedAt(order.getCreatedAt());
        po.setShippedAt(order.getShippedAt());
        po.setSignedAt(order.getSignedAt());
        po.setUpdateTime(order.getUpdateTime());
        return po;
    }

    private FulfillmentLinePo toLinePo(FulfillmentOrder order, FulfillmentLine line) {
        FulfillmentLinePo po = new FulfillmentLinePo();
        po.setFulfillmentId(order.getId().value());
        po.setSkuCode(line.getSkuCode());
        po.setSkuName(line.getSkuName());
        po.setQuantity(line.getQuantity());
        po.setPrice(line.getPrice());
        po.setAmount(line.getAmount());
        po.setCreateTime(LocalDateTime.now());
        return po;
    }
}
