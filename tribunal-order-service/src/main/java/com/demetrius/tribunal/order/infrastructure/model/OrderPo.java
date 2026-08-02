package com.demetrius.tribunal.order.infrastructure.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单持久化对象（infrastructure 层，对应 t_order 表）。
 *
 * <p>注意：PO 是「数据的载体」，与领域聚合 Order 分离——由 RepositoryImpl 负责转换。</p>
 *
 * <p>TODO（学习任务）：对照旧项目 OrderDomain / t_order 表补充字段：</p>
 * <ul>
 *   <li>订单类型（普通/冬储）、配送方式、送货地址</li>
 *   <li>拒绝原因 refuse_reason、失败原因（对照 OrderTransferFail）</li>
 *   <li>版本号 version（乐观锁，并发控制）</li>
 * </ul>
 */
@Data
@TableName("t_order")
public class OrderPo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 订单编号（业务唯一键） */
    private String orderNo;

    private String customerId;

    /** 状态（与 OrderStatus.name() 对应） */
    private String status;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
