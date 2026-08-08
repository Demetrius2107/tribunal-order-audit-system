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
 * <p>TODO（学习任务）：、配送方式、送货地址</li>
 *   <li>拒绝原因 refuse_reason、失败原因（参照通用做法</li>
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

    /** 订单类型（NORMAL/PRE_ORDER） */
    private String orderType;

    /** 是否拼车订单 */
    private Boolean carPooling;

    /** 是否已参与拼车 */
    private Boolean carPoolJoined;

    /** 状态（与 OrderStatus.name() 对应） */
    private String status;

    /** M4：父订单 ID（拆出的子单指向父单；普通单/父单为 null） */
    private String parentOrderId;

    /** M4：是否已被拆分（父单标志） */
    private Boolean split;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    /** 折扣池抵扣 */
    private BigDecimal discountPoolDeduction;

    /** 押金 */
    private BigDecimal depositAmount;

    /** 税费 */
    private BigDecimal taxAmount;

    /** 运费 */
    private BigDecimal shippingFee;

    private BigDecimal payableAmount;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
