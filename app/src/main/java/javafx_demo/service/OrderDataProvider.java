package javafx_demo.service;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx_demo.entity.Order;
import javafx_demo.utils.OrderStatusEnum;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Random;

/**
 * 工单数据提供器 - 生成模拟工单数据
 */
public class OrderDataProvider {
    private static final Random random = new Random();
    private static final String[] PREFIXES = {"订单", "工单", "任务"};

    /**
     * 生成模拟工单数据
     */
    public static ObservableList<Order> generateMockOrders(int count) {
        ObservableList<Order> orders = FXCollections.observableArrayList();
        
        for (int i = 1; i <= count; i++) {
            orders.add(createMockOrder(i));
        }
        
        return orders;
    }

    /**
     * 创建单个模拟工单
     */
    public static Order createMockOrder(int index) {
        String id = String.format("%s%05d", PREFIXES[index % 3], index);
        double price = 50 + random.nextDouble() * 200; // 50-250
        String[] games = {"游戏A", "游戏B", "游戏C", "游戏D", "游戏E"};
        String gameType = games[random.nextInt(games.length)];
        double quantity = 1 + random.nextDouble() * 8;  // 1-9 小时
        
        OrderStatusEnum[] statuses = OrderStatusEnum.values();
        OrderStatusEnum status = statuses[random.nextInt(statuses.length)];
        
        LocalDate startDate = LocalDate.now().minusDays(random.nextInt(30));
        LocalDate endDate = startDate.plusDays(random.nextInt(10) + 1);
        
        return new Order(
            new SimpleStringProperty(id),
            new SimpleDoubleProperty(price),
            new SimpleStringProperty(gameType),
            new SimpleDoubleProperty(quantity),
            new SimpleObjectProperty<>(status),
            new SimpleObjectProperty<>(Date.valueOf(startDate)),
            new SimpleObjectProperty<>(Date.valueOf(endDate))
        );
    }

    /**
     * 创建空白工单
     */
    public static Order createEmptyOrder() {
        return new Order(
            new SimpleStringProperty(""),
            new SimpleDoubleProperty(0),
            new SimpleStringProperty(""),
            new SimpleDoubleProperty(0),
            new SimpleObjectProperty<>(OrderStatusEnum.PENDING),
            new SimpleObjectProperty<>(Date.valueOf(LocalDate.now())),
            new SimpleObjectProperty<>(Date.valueOf(LocalDate.now().plusDays(1)))
        );
    }
}
