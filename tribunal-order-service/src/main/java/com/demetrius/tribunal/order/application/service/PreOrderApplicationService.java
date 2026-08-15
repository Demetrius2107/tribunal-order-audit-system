package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.application.dto.PreOrderActivityResult;
import com.demetrius.tribunal.order.application.dto.PreOrderRecordResult;
import com.demetrius.tribunal.order.domain.model.PreOrderActivity;
import com.demetrius.tribunal.order.domain.model.PreOrderRecord;
import com.demetrius.tribunal.order.domain.repository.PreOrderActivityRepository;
import com.demetrius.tribunal.order.domain.repository.PreOrderRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预购应用服务（F-312：提前采购，经销商预付/保证金模式的用例编排层）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>预购活动配置：创建（草稿）/ 上线 / 结束 / 取消 / 查询</li>
 *   <li>预购订单记录查询：保证金 / 补缴占用明细</li>
 * </ol>
 */
@Service
public class PreOrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PreOrderApplicationService.class);

    private final PreOrderActivityRepository preOrderActivityRepository;

    private final PreOrderRecordRepository preOrderRecordRepository;

    public PreOrderApplicationService(PreOrderActivityRepository preOrderActivityRepository,
                                      PreOrderRecordRepository preOrderRecordRepository) {
        this.preOrderActivityRepository = preOrderActivityRepository;
        this.preOrderRecordRepository = preOrderRecordRepository;
    }

    /**
     * 创建预购活动（初始状态 = 草稿，未上线不可参与）。
     */
    @Transactional
    public PreOrderActivityResult createActivity(String name, List<String> skuCodes,
                                                 BigDecimal depositRate, BigDecimal discountRate,
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        String id = generateId();
        String activityNo = "PRE" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
        PreOrderActivity activity = PreOrderActivity.create(
                id, activityNo, name, skuCodes, depositRate, discountRate, startTime, endTime);
        preOrderActivityRepository.save(activity);
        log.info("创建预购活动 activityNo={} name={}", activityNo, name);
        return PreOrderActivityResult.from(activity);
    }

    /**
     * 上线预购活动（草稿 → 进行中，可参与）。
     */
    @Transactional
    public PreOrderActivityResult activate(String activityNo) {
        PreOrderActivity activity = findRequired(activityNo);
        activity.activate();
        preOrderActivityRepository.save(activity);
        return PreOrderActivityResult.from(activity);
    }

    /**
     * 结束预购活动（进行中 → 已结束，终态）。
     */
    @Transactional
    public PreOrderActivityResult end(String activityNo) {
        PreOrderActivity activity = findRequired(activityNo);
        activity.end();
        preOrderActivityRepository.save(activity);
        return PreOrderActivityResult.from(activity);
    }

    /**
     * 取消预购活动（草稿/进行中 → 已取消，终态）。
     */
    @Transactional
    public PreOrderActivityResult cancel(String activityNo) {
        PreOrderActivity activity = findRequired(activityNo);
        activity.cancel();
        preOrderActivityRepository.save(activity);
        return PreOrderActivityResult.from(activity);
    }

    /**
     * 查询预购活动。
     */
    @Transactional(readOnly = true)
    public PreOrderActivityResult getActivity(String activityNo) {
        return PreOrderActivityResult.from(findRequired(activityNo));
    }

    /**
     * 查询预购订单记录（按活动编号 + 订单号）。
     */
    @Transactional(readOnly = true)
    public PreOrderRecordResult getRecord(String activityNo, String orderNo) {
        return preOrderRecordRepository.findByActivityNoAndOrderNo(activityNo, orderNo)
                .map(PreOrderRecordResult::from)
                .orElseThrow(() -> new BizException("200015", "预购订单记录不存在: " + activityNo + "/" + orderNo));
    }

    private PreOrderActivity findRequired(String activityNo) {
        return preOrderActivityRepository.findByActivityNo(activityNo)
                .orElseThrow(() -> new BizException("200014", "预购活动不存在: " + activityNo));
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
