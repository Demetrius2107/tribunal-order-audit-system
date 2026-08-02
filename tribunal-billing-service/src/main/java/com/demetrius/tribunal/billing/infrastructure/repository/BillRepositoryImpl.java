package com.demetrius.tribunal.billing.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.billing.domain.model.FinanceBill;
import com.demetrius.tribunal.billing.domain.model.BillId;
import com.demetrius.tribunal.billing.domain.model.BillLine;
import com.demetrius.tribunal.billing.domain.model.BillStatus;
import com.demetrius.tribunal.billing.domain.repository.BillRepository;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillLineMapper;
import com.demetrius.tribunal.billing.infrastructure.mapper.BillMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillLinePo;
import com.demetrius.tribunal.billing.infrastructure.model.BillPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 金融账单订单仓储实现（MyBatis-Plus）。
 *
 * <p>TODO（学习任务）：</p>
 * <ul>
 *   <li>接收转单幂等：save 前按 sourceOrderNo 查重（重复转单拒绝）</li>
 *   <li>明细更新策略：先删后插保持事务</li>
 *   <li>乐观锁：PO 加 @Version 防止并发覆盖</li>
 * </ul>
 */
@Repository
public class BillRepositoryImpl implements BillRepository {

    private final BillMapper billMapper;

    private final BillLineMapper billLineMapper;

    public BillRepositoryImpl(BillMapper billMapper, BillLineMapper billLineMapper) {
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
    }

    @Override
    @Transactional
    public void save(FinanceBill order) {
        BillPo po = toPo(order);
        BillPo exist = billMapper.selectOne(
                new LambdaQueryWrapper<BillPo>().eq(BillPo::getSourceOrderNo, order.getSourceOrderNo()));
        if (exist == null) {
            billMapper.insert(po);
        } else {
            billMapper.updateById(po);
        }
        billLineMapper.delete(new LambdaQueryWrapper<BillLinePo>()
                .eq(BillLinePo::getBillId, order.getId().value()));
        for (BillLine line : order.getLines()) {
            billLineMapper.insert(toLinePo(order, line));
        }
    }

    @Override
    public Optional<FinanceBill> findById(BillId id) {
        BillPo po = billMapper.selectById(id.value());
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public Optional<FinanceBill> findBySourceOrderNo(String sourceOrderNo) {
        BillPo po = billMapper.selectOne(
                new LambdaQueryWrapper<BillPo>().eq(BillPo::getSourceOrderNo, sourceOrderNo));
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void delete(BillId id) {
        billMapper.deleteById(id.value());
    }

    // ---------- 转换方法（PO ↔ 领域对象） ----------

    private FinanceBill toDomain(BillPo po) {
        List<BillLinePo> linePos = billLineMapper.findByBillId(po.getId());
        List<BillLine> lines = linePos.stream()
                .map(l -> new BillLine(l.getSkuCode(), l.getSkuName(), l.getQuantity(), l.getPrice()))
                .toList();
        FinanceBill bill = FinanceBill.generate(
                new BillId(po.getId()), po.getSourceOrderNo(), po.getCustomerId(), lines);
        // TODO（学习任务）：还原账单状态/时间戳（对照 Order.restore 思路，后续补充）
        return bill;
    }

    private BillPo toPo(FinanceBill bill) {
        BillPo po = new BillPo();
        po.setId(bill.getId().value());
        po.setSourceOrderNo(bill.getSourceOrderNo());
        po.setCustomerId(bill.getCustomerId());
        po.setStatus(bill.getStatus().name());
        po.setTotalAmount(bill.getTotalAmount());
        po.setGeneratedAt(bill.getGeneratedAt());
        po.setConfirmedAt(bill.getConfirmedAt());
        po.setSettledAt(bill.getSettledAt());
        po.setUpdateTime(bill.getUpdateTime());
        return po;
    }

    private BillLinePo toLinePo(FinanceBill bill, BillLine line) {
        BillLinePo po = new BillLinePo();
        po.setBillId(bill.getId().value());
        po.setSkuCode(line.getSkuCode());
        po.setSkuName(line.getSkuName());
        po.setQuantity(line.getQuantity());
        po.setPrice(line.getPrice());
        po.setAmount(line.getAmount());
        po.setCreateTime(LocalDateTime.now());
        return po;
    }
}
