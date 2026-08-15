package com.demetrius.tribunal.order.application.service;

import com.demetrius.tribunal.common.exception.BizException;
import com.demetrius.tribunal.order.application.dto.CarPoolGroupResult;
import com.demetrius.tribunal.order.domain.model.CarPoolGroup;
import com.demetrius.tribunal.order.domain.model.Order;
import com.demetrius.tribunal.order.domain.model.OrderId;
import com.demetrius.tribunal.order.domain.repository.CarPoolGroupRepository;
import com.demetrius.tribunal.order.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 拼车应用服务（F-310：多订单合并一车运输的用例编排层）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>发起拼车组（初始 OPEN）</li>
 *   <li>加入拼车组：校验成员订单为拼车订单（carPooling=true）且未被拆分</li>
 *   <li>确认拼车：成员订单全部标记已参与拼车（carPoolJoined=true，此后不可单独关闭）</li>
 *   <li>关闭 / 取消拼车组</li>
 *   <li>查询拼车组</li>
 * </ol>
 */
@Service
public class CarPoolApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CarPoolApplicationService.class);

    private final CarPoolGroupRepository carPoolGroupRepository;

    private final OrderRepository orderRepository;

    public CarPoolApplicationService(CarPoolGroupRepository carPoolGroupRepository,
                                     OrderRepository orderRepository) {
        this.carPoolGroupRepository = carPoolGroupRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * 发起拼车组。
     */
    @Transactional
    public CarPoolGroupResult createGroup() {
        String id = generateId();
        String groupNo = "CP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
        CarPoolGroup group = CarPoolGroup.create(id, groupNo);
        carPoolGroupRepository.save(group);
        log.info("发起拼车组 groupNo={}", groupNo);
        return CarPoolGroupResult.from(group);
    }

    /**
     * 加入拼车组（校验成员订单是拼车订单）。
     */
    @Transactional
    public CarPoolGroupResult join(String groupNo, String orderNo) {
        CarPoolGroup group = findRequiredGroup(groupNo);
        Order order = findRequiredOrder(orderNo);
        if (!order.isCarPooling()) {
            throw new BizException("200010", "非拼车订单不能加入拼车组: " + orderNo);
        }
        if (order.isChildOrder() || order.isSplit()) {
            throw new BizException("200011", "拆分子订单不能参与拼车: " + orderNo);
        }
        group.join(orderNo);
        carPoolGroupRepository.save(group);
        log.info("订单加入拼车组 groupNo={} orderNo={}", groupNo, orderNo);
        return CarPoolGroupResult.from(group);
    }

    /**
     * 确认拼车：成员订单全部标记已参与拼车（此后不可单独关闭）。
     */
    @Transactional
    public CarPoolGroupResult confirm(String groupNo) {
        CarPoolGroup group = findRequiredGroup(groupNo);
        group.confirm();
        for (String memberOrderNo : group.getMemberOrderNos()) {
            Order order = findRequiredOrder(memberOrderNo);
            order.joinCarPool();
            orderRepository.save(order);
        }
        carPoolGroupRepository.save(group);
        log.info("确认拼车 groupNo={} members={}", groupNo, group.getMemberOrderNos());
        return CarPoolGroupResult.from(group);
    }

    /**
     * 关闭拼车组（发车完成，终态）。
     */
    @Transactional
    public CarPoolGroupResult close(String groupNo) {
        CarPoolGroup group = findRequiredGroup(groupNo);
        group.close();
        carPoolGroupRepository.save(group);
        return CarPoolGroupResult.from(group);
    }

    /**
     * 取消拼车组（拼车中/已确认均可取消，终态）。
     */
    @Transactional
    public CarPoolGroupResult cancel(String groupNo) {
        CarPoolGroup group = findRequiredGroup(groupNo);
        group.cancel();
        carPoolGroupRepository.save(group);
        return CarPoolGroupResult.from(group);
    }

    /**
     * 查询拼车组。
     */
    @Transactional(readOnly = true)
    public CarPoolGroupResult getGroup(String groupNo) {
        return CarPoolGroupResult.from(findRequiredGroup(groupNo));
    }

    private CarPoolGroup findRequiredGroup(String groupNo) {
        return carPoolGroupRepository.findByGroupNo(groupNo)
                .orElseThrow(() -> new BizException("200012", "拼车组不存在: " + groupNo));
    }

    private Order findRequiredOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException("200002", "订单不存在: " + orderNo));
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
