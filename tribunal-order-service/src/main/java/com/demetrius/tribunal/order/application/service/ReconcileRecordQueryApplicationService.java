package com.demetrius.tribunal.order.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demetrius.tribunal.order.application.dto.ReconcileRecordPage;
import com.demetrius.tribunal.order.application.dto.ReconcileRecordResult;
import com.demetrius.tribunal.order.application.dto.ReconcileSummaryItem;
import com.demetrius.tribunal.order.infrastructure.mapper.ReconcileRecordMapper;
import com.demetrius.tribunal.order.infrastructure.model.ReconcileRecordPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

    /**
     * 对账差异汇总：按 任务/差异类型/处理状态 分组计数（对账结果产品化）。
     */
    @Transactional(readOnly = true)
    public List<ReconcileSummaryItem> summary() {
        List<Map<String, Object>> rows = reconcileRecordMapper.selectMaps(
                new QueryWrapper<ReconcileRecordPo>()
                        .select("task_code", "record_type", "status", "COUNT(*) AS cnt")
                        .groupBy("task_code", "record_type", "status"));
        return rows.stream()
                .map(r -> new ReconcileSummaryItem(
                        str(r.get("record_type")),
                        str(r.get("status")),
                        r.get("cnt") == null ? 0 : ((Number) r.get("cnt")).longValue()))
                .toList();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
