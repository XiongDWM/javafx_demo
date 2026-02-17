package javafx_demo.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx_demo.entity.Order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 工单数据提供器 - 生成模拟工单数据
 */
public class OrderDataProvider {
    private static final Random random = new Random();
    private static final String[] PREFIXES = {"订单", "工单", "任务"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        String[] types = {"SELF_B", "SELF_G", "BOOKED", "SECOND_HAND", "THIRD_PARTY"};
        String[] statuses = {"PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"};
        String[] units = {"HOUR", "BATTLE", "DAY"};
        String[] customers = {"客户A", "客户B", "客户C", "客户D", "客户E"};

        LocalDate startDate = LocalDate.now().minusDays(random.nextInt(30));
        LocalDate endDate = startDate.plusDays(random.nextInt(10) + 1);

        Order order = new Order();
        order.setOrderId(String.format("%s%05d", PREFIXES[index % 3], index));
        order.setLowIncome(50 + random.nextDouble() * 200);
        order.setAmount(1 + random.nextDouble() * 8);
        order.setType(types[random.nextInt(types.length)]);
        order.setStatus(statuses[random.nextInt(statuses.length)]);
        order.setUnitType(units[random.nextInt(units.length)]);
        order.setCustomer(customers[random.nextInt(customers.length)]);
        order.setIssueDate(startDate.atStartOfDay().format(FMT));
        order.setEndAt(endDate.atStartOfDay().format(FMT));
        return order;
    }

    /**
     * 创建空白工单
     */
    public static Order createEmptyOrder() {
        Order order = new Order();
        order.setStatus("PENDING");
        order.setUnitType("HOUR");
        order.setIssueDate(LocalDateTime.now().format(FMT));
        order.setEndAt(LocalDateTime.now().plusDays(1).format(FMT));
        return order;
    }
}
