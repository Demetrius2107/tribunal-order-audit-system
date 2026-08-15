package com.demetrius.tribunal.order.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 拼车组聚合根（F-310：多订单合并一车运输）。
 *
 * <p>拼车组 = 多个拼车订单的集合，成员订单加入后不可单独关闭
 * （由 Order.cancel 的 carPoolJoined 校验兜底，CARPOOL_CANNOT_BE_CLOSED）。</p>
 *
 * <p>业务规则：</p>
 * <ul>
 *   <li>拼车中（OPEN）可加入成员；确认后成员锁定，不可再加入</li>
 *   <li>确认（CONFIRMED）需至少 2 个成员订单（拼车语义：多订单合一车）</li>
 *   <li>已确认可关闭（发车完成）；拼车中/已确认均可取消</li>
 * </ul>
 */
public class CarPoolGroup {

    /** 拼车最少成员数（拼车 = 多订单合一车） */
    public static final int MIN_MEMBERS = 2;

    private final String id;

    /** 拼车组编号（业务唯一键） */
    private final String groupNo;

    private CarPoolGroupStatus status;

    /** 成员订单编号（顺序保持加入次序） */
    private final List<String> memberOrderNos;

    private final LocalDateTime createTime;

    private LocalDateTime updateTime;

    private CarPoolGroup(String id, String groupNo, CarPoolGroupStatus status,
                         List<String> memberOrderNos, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.groupNo = groupNo;
        this.status = status;
        this.memberOrderNos = memberOrderNos;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /** 工厂：发起拼车组（初始状态 = 拼车中，无成员）。 */
    public static CarPoolGroup create(String id, String groupNo) {
        LocalDateTime now = LocalDateTime.now();
        return new CarPoolGroup(id, groupNo, CarPoolGroupStatus.OPEN,
                new ArrayList<>(), now, now);
    }

    /** 还原工厂：从持久化数据完整还原聚合（仓储读取时使用）。 */
    public static CarPoolGroup restore(String id, String groupNo, CarPoolGroupStatus status,
                                       List<String> memberOrderNos,
                                       LocalDateTime createTime, LocalDateTime updateTime) {
        return new CarPoolGroup(id, groupNo, status,
                new ArrayList<>(memberOrderNos), createTime, updateTime);
    }

    /**
     * 加入拼车组（仅拼车中可加入；同一订单不可重复加入）。
     */
    public void join(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("成员订单编号不能为空");
        }
        if (status != CarPoolGroupStatus.OPEN) {
            throw new IllegalStateException("拼车组不在拼车中状态，不可加入: " + status);
        }
        if (memberOrderNos.contains(orderNo)) {
            throw new IllegalStateException("订单已加入该拼车组: " + orderNo);
        }
        memberOrderNos.add(orderNo);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 确认拼车（拼车中 → 已确认，成员锁定）。
     * 校验至少 {@link #MIN_MEMBERS} 个成员（拼车语义），校验通过后才迁移状态。
     */
    public void confirm() {
        if (memberOrderNos.size() < MIN_MEMBERS) {
            throw new IllegalStateException(
                    "拼车至少需要 " + MIN_MEMBERS + " 个成员订单，当前 " + memberOrderNos.size());
        }
        transitTo(CarPoolGroupStatus.CONFIRMED);
    }

    /** 关闭拼车（已确认 → 已关闭，发车完成，终态）。 */
    public void close() {
        transitTo(CarPoolGroupStatus.CLOSED);
    }

    /** 取消拼车（拼车中/已确认 → 已取消，终态）。 */
    public void cancel() {
        transitTo(CarPoolGroupStatus.CANCELLED);
    }

    /** 统一状态迁移入口（状态机 = 幂等核心）。 */
    private void transitTo(CarPoolGroupStatus target) {
        if (!status.canTransitTo(target)) {
            throw new IllegalStateException("非法状态迁移: " + status + " -> " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
    }

    // ---------- getters ----------

    public String getId() {
        return id;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public CarPoolGroupStatus getStatus() {
        return status;
    }

    public List<String> getMemberOrderNos() {
        return Collections.unmodifiableList(memberOrderNos);
    }

    public int getMemberCount() {
        return memberOrderNos.size();
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
