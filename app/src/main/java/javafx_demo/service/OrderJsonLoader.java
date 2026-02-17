package javafx_demo.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx_demo.entity.Order;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单 JSON 数据加载器
 * 负责从绝对路径加载 JSON 文件并解析成 Order 对象
 */
public class OrderJsonLoader {
    
    private static final String JSON_FILE_PATH = "/Users/xiong/Program/JavaProgram/javafx_demo/app/src/main/resources/order_sample.json";
    
    private static ObservableList<Order> cachedOrders = null;
    
    /**
     * 从 JSON 文件加载工单数据
     * @return 工单列表
     */
    public static ObservableList<Order> loadOrders() {
        System.out.println("\n========== 开始加载工单数据 ==========");
        System.out.println("JSON 文件路径: " + JSON_FILE_PATH);
        
        List<Order> orders = new ArrayList<>();
        
        try {
            // 读取 JSON 文件
            String jsonContent = Files.readString(Paths.get(JSON_FILE_PATH), StandardCharsets.UTF_8);
            System.out.println("✅ 成功读取 JSON 文件，内容长度: " + jsonContent.length());
            
            // 解析 JSON 内容
            orders = parseJsonContent(jsonContent);
            System.out.println("✅ 成功解析 " + orders.size() + " 个工单");
            
            // 打印第一个工单的详细信息
            if (!orders.isEmpty()) {
                Order first = orders.get(0);
                System.out.println("\n第一个工单详情:");
                System.out.println("  ID: " + first.getOrderId());
                System.out.println("  收入: " + first.getLowIncome());
                System.out.println("  类型: " + first.getTypeText());
                System.out.println("  数量: " + first.getAmount());
                System.out.println("  状态: " + first.getStatusText());
                System.out.println("  开始日期: " + first.getIssueDate());
                System.out.println("  结束日期: " + first.getEndAt());
            }
            
        } catch (IOException e) {
            System.err.println("❌ 无法读取 JSON 文件: " + e.getMessage());
            System.err.println("❌ 使用备用模拟数据");
            orders = new ArrayList<>(OrderDataProvider.generateMockOrders(8));
        }
        
        System.out.println("========== 工单数据加载完成 ==========\n");
        
        // 缓存数据
        cachedOrders = FXCollections.observableArrayList(orders);
        return cachedOrders;
    }
    
    /**
     * 获取已缓存的工单数据（不重新加载）
     * @return 工单列表
     */
    public static ObservableList<Order> getCachedOrders() {
        if (cachedOrders == null) {
            return loadOrders();
        }
        return cachedOrders;
    }
    
    /**
     * 清除缓存数据
     */
    public static void clearCache() {
        System.out.println("清除缓存数据");
        cachedOrders = null;
    }
    
    /**
     * 手动解析 JSON 内容
     * @param jsonContent JSON 字符串
     * @return Order 对象列表
     */
    private static List<Order> parseJsonContent(String jsonContent) {
        List<Order> orders = new ArrayList<>();
        
        try {
            // 找到 "orders" 数组
            int ordersIdx = jsonContent.indexOf("\"orders\"");
            if (ordersIdx == -1) {
                System.err.println("❌ 找不到 'orders' 字段");
                return orders;
            }
            
            // 找到数组开始 [
            int arrayStartIdx = jsonContent.indexOf("[", ordersIdx);
            if (arrayStartIdx == -1) {
                System.err.println("❌ 找不到数组开始标记");
                return orders;
            }
            
            // 提取每个工单对象
            int depth = 0;
            int objStart = -1;
            
            for (int i = arrayStartIdx; i < jsonContent.length(); i++) {
                char c = jsonContent.charAt(i);
                
                if (c == '{') {
                    if (objStart == -1) {
                        objStart = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart != -1) {
                        String orderJson = jsonContent.substring(objStart, i + 1);
                        Order order = parseOrderObject(orderJson);
                        if (order != null) {
                            orders.add(order);
                        }
                        objStart = -1;
                    }
                } else if (c == ']' && depth == -1) {
                    break;
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 解析 JSON 异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        return orders;
    }
    
    /**
     * 解析单个工单 JSON 对象
     * @param jsonStr JSON 字符串
     * @return Order 对象
     */
    private static Order parseOrderObject(String jsonStr) {
        try {
            String id = extractField(jsonStr, "id");
            String priceStr = extractField(jsonStr, "price");
            String gameType = extractField(jsonStr, "gameType");
            String quantityStr = extractField(jsonStr, "quantity");
            String statusStr = extractField(jsonStr, "status");
            String startAtStr = extractField(jsonStr, "startAt");
            String endAtStr = extractField(jsonStr, "endAt");
            
            if (id.isEmpty() || startAtStr.isEmpty() || endAtStr.isEmpty()) {
                System.err.println("❌ 工单字段缺失: id=" + id);
                return null;
            }
            
            Order order = new Order();
            order.setOrderId(id);
            order.setLowIncome(parseDouble(priceStr));
            order.setType(gameType);
            order.setAmount(parseDouble(quantityStr));
            order.setStatus(statusStr);
            order.setIssueDate(startAtStr);
            order.setEndAt(endAtStr);
            return order;
            
        } catch (Exception e) {
            System.err.println("❌ 解析工单对象失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 JSON 字符串提取字段值
     * @param json JSON 字符串
     * @param fieldName 字段名
     * @return 字段值
     */
    private static String extractField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx == -1) return "";
        
        int startIdx = colonIdx + 1;
        while (startIdx < json.length() && (json.charAt(startIdx) == ' ' || json.charAt(startIdx) == ',')) {
            startIdx++;
        }
        
        if (startIdx >= json.length()) return "";
        
        char firstChar = json.charAt(startIdx);
        
        if (firstChar == '"') {
            // 字符串值
            int endIdx = json.indexOf("\"", startIdx + 1);
            if (endIdx != -1) {
                return json.substring(startIdx + 1, endIdx);
            }
        } else {
            // 数字值
            int endIdx = startIdx;
            while (endIdx < json.length() && (Character.isDigit(json.charAt(endIdx)) || json.charAt(endIdx) == '.')) {
                endIdx++;
            }
            return json.substring(startIdx, endIdx);
        }
        
        return "";
    }
    
    /**
     * 解析数字字符串
     * @param value 数字字符串
     * @return 解析结果
     */
    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
