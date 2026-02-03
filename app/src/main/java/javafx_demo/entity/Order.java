package javafx_demo.entity;

import java.sql.Date;

import javafx.beans.property.*;
import javafx_demo.utils.OrderStatusEnum;

/**
 * 工单实体类
 */
public record Order(
    StringProperty id, // 工单ID
    DoubleProperty price, // 单价
    StringProperty gameType, // 游戏
    DoubleProperty quantity, // 小时数
    ObjectProperty<OrderStatusEnum> status, // 状态
    ObjectProperty<Date> startAt, // 开始时间
    ObjectProperty<Date> endAt // 结束时间
    
) {
}
