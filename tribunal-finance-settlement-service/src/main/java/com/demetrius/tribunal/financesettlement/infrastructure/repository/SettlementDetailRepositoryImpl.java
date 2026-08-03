package com.demetrius.tribunal.financesettlement.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demetrius.tribunal.financesettlement.domain.model.SettlementDetail;
import com.demetrius.tribunal.financesettlement.domain.repository.SettlementDetailRepository;
import com.demetrius.tribunal.financesettlement.infrastructure.mapper.SettlementDetailMapper;
import com.demetrius.tribunal.financesettlement.infrastructure.model.SettlementDetailPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 结算明细仓储实现（infrastructure 层）。
 */
@Repository
public class SettlementDetailRepositoryImpl implements SettlementDetailRepository {

    private final SettlementDetailMapper settlementDetailMapper;

    public SettlementDetailRepositoryImpl(SettlementDetailMapper settlementDetailMapper) {
        this.settlementDetailMapper = settlementDetailMapper;
    }

    @Override
    public List<SettlementDetail> findBySettlementId(String settlementId) {
        return settlementDetailMapper.selectList(
                        new LambdaQueryWrapper<SettlementDetailPo>()
                                .eq(SettlementDetailPo::getSettlementId, settlementId))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void save(SettlementDetail detail) {
        SettlementDetailPo po = new SettlementDetailPo();
        po.setId(detail.getId());
        po.setSettlementId(detail.getSettlementId());
        po.setItemType(detail.getItemType());
        po.setSkuId(detail.getSkuId());
        po.setSkuName(detail.getSkuName());
        po.setQuantity(detail.getQuantity());
        po.setUnitPrice(detail.getUnitPrice());
        po.setOriginalAmount(detail.getOriginalAmount());
        po.setActualAmount(detail.getActualAmount());
        po.setDescription(detail.getDescription());
        settlementDetailMapper.insert(po);
    }

    private SettlementDetail toDomain(SettlementDetailPo po) {
        return new SettlementDetail(
                po.getId(), po.getSettlementId(), po.getItemType(), po.getSkuId(), po.getSkuName(),
                po.getQuantity(), po.getUnitPrice(), po.getOriginalAmount(), po.getActualAmount(),
                po.getDescription());
    }
}
