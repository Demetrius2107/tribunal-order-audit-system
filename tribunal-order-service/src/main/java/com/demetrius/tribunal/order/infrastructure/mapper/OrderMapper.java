package com.demetrius.tribunal.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.order.infrastructure.model.OrderPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper（MyBatis-Plus，基础 CRUD 由 BaseMapper 提供）。
 *
 * <p>TODO（学习任务）：对照旧项目 OrderDao，补充复杂查询：</p>
 * <ul>
 *   <li>按条件分页查询（状态/客户/时间，对照旧项目 orderPo 查询包装）</li>
 *   <li>状态批量更新（对照 batchConfirmOrder / batchUpdateOrder）</li>
 *   <li>自定义 SQL（XML 或 @Select 注解）</li>
 * </ul>
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderPo> {
}
