package javafx_demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx_demo.entity.Order;
import javafx_demo.service.OrderDataProvider;
import javafx_demo.service.OrderJsonLoader;
import javafx_demo.utils.OrderStatusEnum;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * 工单表单控制器
 */
public class OrderFormController {

    @FXML
    private TableView<Order> orderTable;

    @FXML
    private TextField searchField;

    @FXML
    private Button addBtn;

    @FXML
    private Button editBtn;

    @FXML
    private Button deleteBtn;

    @FXML
    private Button searchBtn;

    @FXML
    private Label statusLabel;

    @FXML
    private Label countLabel;

    @FXML
    private TableColumn<Order, String> idCol;
    
    @FXML
    private TableColumn<Order, Double> priceCol;
    
    @FXML
    private TableColumn<Order, String> gameCol;
    
    @FXML
    private TableColumn<Order, Double> quantityCol;
    
    @FXML
    private TableColumn<Order, OrderStatusEnum> statusCol;
    
    @FXML
    private TableColumn<Order, Date> startCol;
    
    @FXML
    private TableColumn<Order, Date> endCol;
    
    @FXML
    private TableColumn<Order, Void> actionCol;

    private ObservableList<Order> allOrders;
    private ObservableList<Order> filteredOrders;

    @FXML
    public void initialize() {
        System.out.println("\n========== OrderFormController 初始化开始 ==========");
        try {
            System.out.println("初始表格列数: " + orderTable.getColumns().size());
            
            // 1. 从缓存加载工单数据（登录时已加载）
            System.out.println("step 1: 从缓存加载工单数据...");
            allOrders = OrderJsonLoader.getCachedOrders();
            System.out.println("step 1: ✅ 已加载 " + allOrders.size() + " 个工单");
            
            // 打印第一条工单的信息用于调试
            if (!allOrders.isEmpty()) {
                Order first = allOrders.get(0);
                System.out.println("第一条工单: id=" + first.id().getValue() + 
                                   ", price=" + first.price().getValue() +
                                   ", quantity=" + first.quantity().getValue());
            }
            
            // 2. 创建过滤列表
            System.out.println("step 2: 创建过滤列表...");
            filteredOrders = FXCollections.observableArrayList(allOrders);
            System.out.println("step 2: ✅ 过滤列表创建完成");
            
            // 3. 配置表格列
            System.out.println("step 3: 配置表格列...");
            setupTableColumns();
            System.out.println("step 3: ✅ 表格列配置完成");
            
            // 4. 设置Table的数据
            System.out.println("step 4: 绑定表格数据...");
            orderTable.setItems(filteredOrders);
            System.out.println("step 4: ✅ 表格数据绑定完成");
            System.out.println("   -> 表格现在有 " + orderTable.getItems().size() + " 行数据");
            
            // 5. 更新状态统计
            System.out.println("step 5: 更新状态统计...");
            updateStatus();
            System.out.println("step 5: ✅ 状态统计更新完成");
            
            System.out.println("========== OrderFormController 初始化成功！ ==========\n");
            
        } catch (Exception e) {
            System.err.println("❌ OrderFormController 初始化失败: " + e.getMessage());
            e.printStackTrace();
            
            // 最后的备选方案
            filteredOrders = FXCollections.observableArrayList();
            orderTable.setItems(filteredOrders);
        }
    }

    /**
     * 配置表格列，为每列设置正确的 CellValueFactory
     */
    private void setupTableColumns() {
        System.out.println("\n开始配置表格列...");
        
        try {
            // 为每一列配置 CellValueFactory，使用 lambda 访问 record 的 accessor 方法
            idCol.setCellValueFactory(cellData -> cellData.getValue().id());
            System.out.println("✅ 已配置 ID 列");
            
            // DoubleProperty 需要转换为 ObservableValue<Double>
            priceCol.setCellValueFactory(cellData -> cellData.getValue().price().asObject());
            System.out.println("✅ 已配置 Price 列");
            
            gameCol.setCellValueFactory(cellData -> cellData.getValue().gameType());
            System.out.println("✅ 已配置 GameType 列");
            
            // DoubleProperty 需要转换为 ObservableValue<Double>
            quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantity().asObject());
            System.out.println("✅ 已配置 Quantity 列");
            
            statusCol.setCellValueFactory(cellData -> cellData.getValue().status());
            System.out.println("✅ 已配置 Status 列");
            
            startCol.setCellValueFactory(cellData -> cellData.getValue().startAt());
            System.out.println("✅ 已配置 StartAt 列");
            
            endCol.setCellValueFactory(cellData -> cellData.getValue().endAt());
            System.out.println("✅ 已配置 EndAt 列");
            
            // 配置操作列
            setupActionColumn();
            System.out.println("✅ 已配置 Action 列");
            
            System.out.println("表格列配置完成！\n");
            
        } catch (Exception e) {
            System.err.println("❌ 配置表格列失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置操作列
     */
    private void setupActionColumn() {
        TableColumn<Order, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setPrefWidth(120.0);
        actionColumn.setStyle("-fx-alignment: CENTER;");
        
        actionColumn.setCellFactory(param -> new TableCell<Order, Void>() {
            private final Button viewBtn = new Button("查看");
            private final Button renewBtn = new Button("续单");
            
            {
                viewBtn.setStyle("-fx-font-size: 10; -fx-padding: 3 10 3 10;");
                viewBtn.setOnAction(event -> handleViewAction());
                
                renewBtn.setStyle("-fx-font-size: 10; -fx-padding: 3 10 3 10; -fx-background-color: #f39c12; -fx-text-fill: white;");
                renewBtn.setOnAction(event -> handleRenewAction());
                
                HBox hbox = new HBox(5);
                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(viewBtn, renewBtn);
                setGraphic(hbox);
            }
            
            private void handleViewAction() {
                Order selected = getTableView().getItems().get(getIndex());
                handleViewForOrder(selected);
            }
            
            private void handleRenewAction() {
                Order selected = getTableView().getItems().get(getIndex());
                handleRenewForOrder(selected);
            }
        });
        
        orderTable.getColumns().add(actionColumn);
    }

    /**
     * 从JSON文件加载工单数据
     */
    private ObservableList<Order> loadOrdersFromJson() {
        ObservableList<Order> orders = FXCollections.observableArrayList();
        
        try {
            InputStream inputStream = getClass().getResourceAsStream("/order_sample.json");
            if (inputStream == null) {
                System.err.println("❌ 无法加载JSON文件");
                return orders;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String jsonContent = reader.lines().collect(Collectors.joining("\n"));
            reader.close();

            System.out.println("✅ JSON文件已读取，内容长度: " + jsonContent.length());
            System.out.println("JSON内容前200字: " + jsonContent.substring(0, Math.min(200, jsonContent.length())));

            // 解析JSON并创建Order对象
            parseJsonOrders(jsonContent, orders);
            System.out.println("✅ 解析完成，获得工单数: " + orders.size());
            
            // 输出第一个工单的详细信息
            if (!orders.isEmpty()) {
                Order firstOrder = orders.get(0);
                System.out.println("第一个工单详情:");
                System.out.println("  ID: " + firstOrder.id().get());
                System.out.println("  Price: " + firstOrder.price().get());
                System.out.println("  GameType: " + firstOrder.gameType().get());
                System.out.println("  Quantity: " + firstOrder.quantity().get());
                System.out.println("  Status: " + firstOrder.status().get());
                System.out.println("  StartAt: " + firstOrder.startAt().get());
                System.out.println("  EndAt: " + firstOrder.endAt().get());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ 加载JSON文件失败: " + e.getMessage());
        }
        
        return orders;
    }

    /**
     * 解析JSON数组并创建Order对象
     */
    private void parseJsonOrders(String jsonContent, ObservableList<Order> orders) {
        try {
            int ordersStart = jsonContent.indexOf("\"orders\":");
            System.out.println("查找 orders 数组，位置: " + ordersStart);
            if (ordersStart == -1) {
                System.err.println("❌ 找不到 orders 数组");
                return;
            }
            
            int arrayStart = jsonContent.indexOf("[", ordersStart);
            int arrayEnd = jsonContent.lastIndexOf("]");
            System.out.println("数组开始位置: " + arrayStart + "，结束位置: " + arrayEnd);
            if (arrayStart == -1 || arrayEnd == -1) {
                System.err.println("❌ 找不到数组括号");
                return;
            }
            
            String ordersArray = jsonContent.substring(arrayStart + 1, arrayEnd);
            System.out.println("提取的数组内容长度: " + ordersArray.length());
            System.out.println("数组内容前100字: " + ordersArray.substring(0, Math.min(100, ordersArray.length())));
            
            int depth = 0;
            int start = 0;
            int objectCount = 0;
            
            for (int i = 0; i < ordersArray.length(); i++) {
                char c = ordersArray.charAt(i);
                
                if (c == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        String orderObj = ordersArray.substring(start + 1, i);
                        System.out.println("解析第 " + (objectCount + 1) + " 个工单JSON对象");
                        Order order = parseOrderObject(orderObj);
                        if (order != null) {
                            orders.add(order);
                            System.out.println("✅ 添加工单: " + order.id().get());
                        } else {
                            System.err.println("❌ 解析工单对象失败");
                        }
                        objectCount++;
                    }
                }
            }
            System.out.println("共解析 " + objectCount + " 个工单对象，成功添加 " + orders.size() + " 个");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析单个JSON对象并创建Order
     */
    private Order parseOrderObject(String orderObj) {
        try {
            String id = extractJsonValue(orderObj, "id");
            String priceStr = extractJsonValue(orderObj, "price");
            String gameType = extractJsonValue(orderObj, "gameType");
            String quantityStr = extractJsonValue(orderObj, "quantity");
            String statusStr = extractJsonValue(orderObj, "status");
            String startAtStr = extractJsonValue(orderObj, "startAt");
            String endAtStr = extractJsonValue(orderObj, "endAt");
            
            System.out.println("  提取的值: ID=" + id + ", price=" + priceStr + ", gameType=" + gameType);
            
            if (id.isEmpty() || priceStr.isEmpty() || startAtStr.isEmpty()) {
                System.err.println("  ❌ 工单字段缺失: id=" + id + ", price=" + priceStr + ", startAt=" + startAtStr);
                return null;
            }
            
            double price = Double.parseDouble(priceStr);
            double quantity = quantityStr.isEmpty() ? 0 : Double.parseDouble(quantityStr);
            
            OrderStatusEnum status = OrderStatusEnum.valueOf(statusStr);
            
            Date startAt = Date.valueOf(startAtStr);
            Date endAt = Date.valueOf(endAtStr);
            
            System.out.println("  ✅ 成功创建 Order 对象");
            
            return new Order(
                new SimpleStringProperty(id),
                new SimpleDoubleProperty(price),
                new SimpleStringProperty(gameType),
                new SimpleDoubleProperty(quantity),
                new SimpleObjectProperty<>(status),
                new SimpleObjectProperty<>(startAt),
                new SimpleObjectProperty<>(endAt)
            );
        } catch (Exception e) {
            System.err.println("  ❌ 解析工单对象失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从JSON字符串中提取字段值
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return "";
        
        int colonIndex = json.indexOf(":", keyIndex);
        int commaIndex = json.indexOf(",", colonIndex);
        if (commaIndex == -1) commaIndex = json.length();
        
        String value = json.substring(colonIndex + 1, commaIndex).trim();
        value = value.replaceAll("^\"|\"$", "").trim();
        
        return value;
    }

    @FXML
    private void handleAddOrder() {
        System.out.println("新建工单");
        Order newOrder = OrderDataProvider.createEmptyOrder();
        allOrders.add(newOrder);
        filteredOrders.add(newOrder);
        updateStatus();
        showInfo("新建工单", "成功创建新工单");
    }

    @FXML
    private void handleEditOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("提示", "请先选择要编辑的工单");
            return;
        }
        System.out.println("编辑工单: " + selected.id().get());
        showInfo("编辑工单", "编辑工单: " + selected.id().get() + "\n功能开发中...");
    }

    @FXML
    private void handleDeleteOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("提示", "请先选择要删除的工单");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除工单 " + selected.id().get() + " 吗?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                allOrders.remove(selected);
                filteredOrders.remove(selected);
                updateStatus();
                System.out.println("删除工单: " + selected.id().get());
            }
        });
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            filteredOrders.setAll(allOrders);
        } else {
            filteredOrders.clear();
            for (Order order : allOrders) {
                if (order.id().get().contains(keyword)) {
                    filteredOrders.add(order);
                }
            }
        }
        updateStatus();
    }

    private void handleViewForOrder(Order selected) {
        if (selected != null) {
            StringBuilder details = new StringBuilder();
            details.append("工单ID: ").append(selected.id().get()).append("\n");
            details.append("单价: ¥").append(String.format("%.2f", selected.price().get())).append("\n");
            details.append("游戏: ").append(selected.gameType().get()).append("\n");
            details.append("小时数: ").append(String.format("%.2f", selected.quantity().get())).append("\n");
            details.append("状态: ").append(selected.status().get()).append("\n");
            details.append("开始日期: ").append(selected.startAt().get()).append("\n");
            details.append("结束日期: ").append(selected.endAt().get()).append("\n");
            details.append("总价: ¥").append(String.format("%.2f", 
                selected.price().get() * selected.quantity().get()));
            
            showInfo("工单详情", details.toString());
        }
    }

    private void handleRenewForOrder(Order selected) {
        if (selected == null) {
            showWarning("提示", "请先选择要续单的工单");
            return;
        }
        
        System.out.println("续单: " + selected.id().get());
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认续单");
        alert.setHeaderText(null);
        alert.setContentText("确定要续单工单 " + selected.id().get() + " 吗?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Order renewedOrder = new Order(
                    new SimpleStringProperty(selected.id().get() + "-续"),
                    new SimpleDoubleProperty(selected.price().get()),
                    new SimpleStringProperty(selected.gameType().get()),
                    new SimpleDoubleProperty(selected.quantity().get()),
                    new SimpleObjectProperty<>(OrderStatusEnum.PENDING),
                    new SimpleObjectProperty<>(Date.valueOf(LocalDate.now())),
                    new SimpleObjectProperty<>(Date.valueOf(LocalDate.now().plusDays(7)))
                );
                
                allOrders.add(renewedOrder);
                filteredOrders.add(renewedOrder);
                updateStatus();
                showInfo("续单成功", "新工单ID: " + renewedOrder.id().get());
            }
        });
    }

    private void updateStatus() {
        statusLabel.setText("已加载 " + filteredOrders.size() + " 条记录");
        countLabel.setText("总计: " + allOrders.size() + " 个工单");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
