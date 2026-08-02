package com.demetrius.tribunal.order.domain.repository;

import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;

import java.util.Optional;

/**
 * 订单仓储接口（★依赖倒置核心）。
 *
 * <p>接口定义在领域层，实现放在基础设施层（OrderRepositoryImpl）。
 * 领域层只依赖接口，不依赖 MyBatis/JPA 等任何持久化技术。</p>
 *
 * <p>TODO（学习任务）：对照旧项目 OrderDao / OrderSkuDao：</p>
 * <ul>
 *   <li>思考：save 时聚合内的 skus 如何一起持久化（事务边界）</li>
 *   <li>补充分页查询接口（对照旧项目 pagehelper 用法）</li>
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
     * 删除订单（物理删除 / 逻辑删除由实现决定，推荐逻辑删除）。
     */
    void delete(OrderId id);
}
