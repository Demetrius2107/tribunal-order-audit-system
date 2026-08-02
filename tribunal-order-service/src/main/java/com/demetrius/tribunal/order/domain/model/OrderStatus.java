package com.demetrius.tribunal.order.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机（★核心学习点）
 *
 * <p>对照旧项目：{@code OrderConstants} 中的状态流（getStateFlow / getD365OrderMapping）
 * 与 {@code OrderSynchronizationStatusHandle.confirmationOfStatus} 的状态前置校验逻辑。</p>
 *
 * <p>设计要点：</p>
 * <ol>
 *   <li>每个状态只允许迁移到 {@code TRANSITIONS} 中定义的目标状态</li>
 *   <li>状态重复回传（旧项目"当前订单状态已经同步过"）由「目标状态不在 TRANSITIONS 中」天然拦截</li>
 *   <li>状态机是幂等的核心保障：非法迁移直接抛异常，数据库状态不变</li>
 * </ol>
 *
 * <p>实现说明：Java 枚举常量不能在构造参数中前向引用后续定义的常量（非法前向引用），
 * 因此状态迁移表用「静态块 + EnumMap」构建——静态块执行时所有常量都已定义完毕。</p>
 *
 * <p>TODO（学习任务）：对照旧项目补充更多业务状态：</p>
 * <ul>
 *   <li>信用检查中 / 信用释放（旧项目 ORDER_RELEASE）</li>
 *   <li>部分发货（旧项目 PART_SHIPMENT）</li>
 *   <li>冬储单终止（旧项目 ORDER_STOP，行业特有，可选）</li>
 *   <li>思考：哪些状态允许"重复触发"（旧项目 CONFIRM_AN_ORDER 允许 005/301 前置）</li>
 * </ul>
 */
public enum OrderStatus {

    /** 待确认（下单初始状态） */
    TO_BE_CONFIRMED("待确认"),

    /** 已确认（审单通过） */
    CONFIRMED("已确认"),

    /** 转单中（已发给 D365 / 外部系统） */
    TRANSFERRING("转单中"),

    /** 已转单 */
    TRANSFERRED("已转单"),

    /** 已发货 */
    SHIPPED("已发货"),

    /** 已签收（终态） */
    SIGNED("已签收"),

    /** 已拒绝（审单拒绝，终态） */
    REJECTED("已拒绝"),

    /** 已取消（终态） */
    CANCELLED("已取消");

    private final String desc;

    /** 状态迁移表：当前状态 → 允许迁移的目标状态集合 */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(TO_BE_CONFIRMED, EnumSet.of(CONFIRMED, REJECTED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(TRANSFERRING, CANCELLED));
        TRANSITIONS.put(TRANSFERRING, EnumSet.of(TRANSFERRED, CANCELLED));
        TRANSITIONS.put(TRANSFERRED, EnumSet.of(SHIPPED, CANCELLED));
        TRANSITIONS.put(SHIPPED, EnumSet.of(SIGNED));
        // 终态：不可再迁移
        TRANSITIONS.put(SIGNED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    OrderStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 校验是否允许迁移到目标状态。
     *
     * <p>TODO（学习任务）：这里目前是纯集合判断，思考两个升级点：</p>
     * <ol>
     *   <li>是否需要"前置状态允许有多个"的扩展（旧项目发货完成允许 101/105 前置）</li>
     *   <li>是否引入"迁移动作"（如迁移到 SHIPPED 前必须执行库存/物流校验）——
     *       可参考策略模式把迁移动作挂到每个枚举值上</li>
     * </ol>
     *
     * @param target 目标状态
     * @return true 允许迁移
     */
    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    @Override
    public String toString() {
        return name() + "(" + desc + ")";
    }
}
