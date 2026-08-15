package com.demetrius.tribunal.order.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demetrius.tribunal.order.application.dto.ReconcileRecordPage;
import com.demetrius.tribunal.order.application.dto.ReconcileRecordResult;
import com.demetrius.tribunal.order.infrastructure.mapper.ReconcileRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对账差异记录查询应用服务（F-801/F-802：对账结果产品化）。
 *
 * <p>对账任务写入的差异记录（t_reconcile_record）分页查询，供运营查看差异明细。</p>
 */
@Service
public class ReconcileRecordQueryApplicationService {

    private final ReconcileRecordMapper reconcileRecordMapper;

    public ReconcileRecordQueryApplicationService(ReconcileRecordMapper reconcileRecordMapper) {
        this.reconcileRecordMapper = reconcileRecordMapper;
    }

    /**
     * 分页查询对账差异记录（可按任务/类型/状态过滤，时间倒序）。
     */
    @Transactional(readOnly = true)
    public ReconcileRecordPage query(String taskCode, String recordType, String status,
                                     long pageNum, long pageSize) {
        LambdaQueryWrapper<ReconcileRecordPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskCode != null && !taskCode.isBlank(), ReconcileRecordPo::getTaskCode, taskCode)
               .eq(recordType != null && !recordType.isBlank(), ReconcileRecordPo::getRecordType, recordType)
               .eq(status != null && !status.isBlank(), ReconcileRecordPo::getStatus, status)
               .orderByDesc(ReconcileRecordPo::getCreateTime);

        Page<ReconcileRecordPo> page = reconcileRecordMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<ReconcileRecordResult> records = page.getRecords().stream()
                .map(ReconcileRecordResult::from)
                .toList();
        return ReconcileRecordPage.of(page.getTotal(), pageNum, pageSize, records);
    }
}
