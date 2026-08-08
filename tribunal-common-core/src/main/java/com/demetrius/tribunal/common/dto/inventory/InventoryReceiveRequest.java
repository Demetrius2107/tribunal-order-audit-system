package com.demetrius.tribunal.common.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 上游库存系统推送报文（对应 PRD 4.1 POST /api/v1/inventory/receive）。
 *
 * <p>承载一次全量/增量推送批次：批次号 + 推送类型 + 明细条目列表。</p>
 */
@Data
public class InventoryReceiveRequest {

    /** 推送批次号，如 INV_20260803_001 */
    @NotBlank(message = "batchId 不能为空")
    private String batchId;

    /** 推送类型：FULL 全量 / INCREMENTAL 增量 */
    @NotBlank(message = "pushType 不能为空")
    private String pushType;

    /** 推送时间（ISO-8601，UTC+8） */
    @NotBlank(message = "timestamp 不能为空")
    private String timestamp;

    /** 本次推送总条数 */
    @NotNull(message = "totalCount 不能为空")
    private Integer totalCount;

    /** 明细条目 */
    @NotEmpty(message = "items 不能为空")
    @Valid
    private List<Item> items;

    /**
     * 单条库存明细（含库存快照与批次信息）。
     */
    @Data
    public static class Item {

        /** 上游物料编码 */
        @NotBlank(message = "sourceSkuId 不能为空")
        private String sourceSkuId;

        /** 仓库编码 */
        @NotBlank(message = "warehouseId 不能为空")
        private String warehouseId;

        /** 货主编码 */
        @NotBlank(message = "ownerId 不能为空")
        private String ownerId;

        /** 库存快照（各维度数量） */
        @NotNull(message = "inventory 不能为空")
        @Valid
        private InventorySnapshot inventory;

        /** 批次信息（可选，按批次管理时必填） */
        private InventoryBatchInfo batchInfo;

        /** 版本号（乐观锁/顺序控制，大版本覆盖小版本） */
        @NotNull(message = "version 不能为空")
        private Long version;

        /** 上游单位，如 BOX */
        private String unit;

        /** 单位换算系数，如 1箱=10件 */
        private Integer unitConversion;
    }
}
