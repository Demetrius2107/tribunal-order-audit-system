package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储接口（★依赖倒置核心）。
 *
 * <p>接口定义在领域层，实现放在基础设施层（OrderRepositoryImpl）。
 * 领域层只依赖接口，不依赖 MyBatis/JPA 等任何持久化技术。</p>
 *
 * <p>TODO（学习任务）：参照通用做法</li>
 *   <li>思考：查询返回聚合还是返回数据对象？DDD 要求返回聚合（完整还原）</li>
 * </ul>
 */
public interface OrderRepository {

    /**
     * 保存订单聚合（新增 + 修改）。
     *
     * <p>约定：由调用方（应用服务）保证事务；实现内负责 order 主表 + order_sku 明细的落库。</p>
     */
    void save(Order order);

    /**
     * 按 ID 查询订单聚合（含明细）。
     */
    Optional<Order> findById(OrderId id);

    /**
     * 按订单编号查询（业务唯一键，用于幂等判断）。
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 分页查询订单列表（可按客户/状态过滤；customerId、status 为空时不参与过滤）。
     */
    OrderPage findPage(String customerId, String status, long pageNum, long pageSize);

    /**
     * 删除订单（物理删除 / 逻辑删除由实现决定，推荐逻辑删除）。
     */
    void delete(OrderId id);

    /**
     * M4：按父订单 ID 查询所有子单（拆单后状态聚合时使用）。
     */
    List<Order> findByParentOrderId(String parentOrderId);

    /**
     * 超时关单：查询指定状态且创建时间早于截止时间的订单（时间升序，限量）。
     */
    List<Order> findTimeoutOrders(OrderStatus status, java.time.LocalDateTime before, int limit);
}
