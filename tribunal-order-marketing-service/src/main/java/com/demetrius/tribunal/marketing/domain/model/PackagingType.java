package com.demetrius.tribunal.marketing.domain.model;

/**
 * 包装类型（啤酒经销五大包装类型，对应五类押金）。
 *
 * <p>每种 SKU 以特定包装形式销售，押金按包装类型配置。</p>
 */
public enum PackagingType {
    /** 瓶装 */
    BOTTLE,
    /** 箱装 */
    BOX,
    /** 桶装（鲜啤 keg） */
    KEG,
    /** 托盘（整托发货） */
    TRAY,
    /** 坛装（特色酒） */
    JAR
}
