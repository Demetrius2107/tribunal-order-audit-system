package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.PreOrderRecord;

import java.util.Optional;

/**
 * 预购订单记录仓储接口（F-312：domain 定义，infrastructure 实现）。
 */
public interface PreOrderRecordRepository {

    /** 保存预购订单记录（活动编号 + 订单号唯一）。 */
    void save(PreOrderRecord record);

    /** 按活动编号 + 订单号查询。 */
    Optional<PreOrderRecord> findByActivityNoAndOrderNo(String activityNo, String orderNo);

    /** 按订单号删除（订单关闭时删除预购占用记录，业务文档七节）。 */
    void deleteByOrderNo(String orderNo);
}
