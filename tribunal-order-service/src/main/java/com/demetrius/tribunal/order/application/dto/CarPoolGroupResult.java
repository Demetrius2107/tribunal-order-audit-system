package com.demetrius.tribunal.order.application.dto;

import com.demetrius.tribunal.order.domain.model.CarPoolGroup;
import com.demetrius.tribunal.order.domain.model.CarPoolGroupStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼车组应用层出参（F-310）。
 */
public record CarPoolGroupResult(
        String groupId,
        String groupNo,
        CarPoolGroupStatus status,
        List<String> memberOrderNos,
        int memberCount,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    /** 聚合 → 应用层 DTO。 */
    public static CarPoolGroupResult from(CarPoolGroup group) {
        return new CarPoolGroupResult(
                group.getId(),
                group.getGroupNo(),
                group.getStatus(),
                group.getMemberOrderNos(),
                group.getMemberCount(),
                group.getCreateTime(),
                group.getUpdateTime());
    }
}
