package com.demetrius.tribunal.billing.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demetrius.tribunal.billing.infrastructure.model.BillLinePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 金融账单订单明细 Mapper。
 */
@Mapper
public interface BillLineMapper extends BaseMapper<BillLinePo> {

    @Select("SELECT * FROM t_bill_line WHERE bill_id = #{billId} AND deleted = 0")
    List<BillLinePo> findByBillId(@Param("billId") String billId);
}
