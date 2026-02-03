package javafx_demo.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx_demo.entity.Order;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.SceneManager;
import javafx_demo.service.OrderJsonLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Date;

/**
 * Main Controller - 主页面控制器
 */
public class MainController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Button logoutButton;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button userInfoBtn;

    @FXML
    private Button dataManageBtn;

    @FXML
    private Button settingsBtn;

    @FXML
    private Button suspendBtn;

    @FXML
    private Button readyBtn;

    @FXML
    private TableView<Order> ordersTable;

    @FXML
    private TableColumn<Order, String> idCol;

    @FXML
    private TableColumn<Order, Double> priceCol;

    @FXML
    private TableColumn<Order, String> gameTypeCol;

    @FXML
    private TableColumn<Order, Double> quantityCol;

    @FXML
    private TableColumn<Order, String> statusCol;

    @FXML
    private TableColumn<Order, String> startAtCol;

    @FXML
    private TableColumn<Order, String> endAtCol;

    @FXML
    private TableColumn<Order, Void> actionCol;

    @FXML
    private Label statusLabel;

    @FXML
    private Label versionLabel;

    private List<Button> menuButtons;
    private String currentUsername;
    private ObservableList<Order> ordersList;

    @FXML
    public void initialize() {
        ConfigManager config = ConfigManager.getInstance();
        versionLabel.setText("Version " + config.getAppVersion());
        
        // 收集所有菜单按钮
        menuButtons = Arrays.asList(dashboardBtn, userInfoBtn, dataManageBtn, settingsBtn);
        
        // 初始化表格列
        setupTableColumns();
        
        // 加载工单数据
        loadOrders();
        
        // 默认显示仪表盘
        showDashboard();
    }

    /**
     * 设置表格列绑定
     */
    private void setupTableColumns() {
        System.out.println("\n========== MainController setupTableColumns 开始 ==========");
        
        // 绑定所有 Order 字段到表格列
        idCol.setCellValueFactory(cellData -> cellData.getValue().id());
        priceCol.setCellValueFactory(cellData -> cellData.getValue().price().asObject());
        gameTypeCol.setCellValueFactory(cellData -> cellData.getValue().gameType());
        quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantity().asObject());
        
        // 状态列 - 显示 OrderStatusEnum 的中文名
        statusCol.setCellValueFactory(cellData -> {
            javafx_demo.utils.OrderStatusEnum status = cellData.getValue().status().getValue();
            String statusText = status != null ? getStatusLabel(status) : "未知";
            return new javafx.beans.property.SimpleStringProperty(statusText);
        });
        
        // 时间列 - 格式化日期
        startAtCol.setCellValueFactory(cellData -> {
            java.sql.Date date = cellData.getValue().startAt().getValue();
            return new javafx.beans.property.SimpleStringProperty(date != null ? date.toString() : "");
        });
        
        endAtCol.setCellValueFactory(cellData -> {
            java.sql.Date date = cellData.getValue().endAt().getValue();
            return new javafx.beans.property.SimpleStringProperty(date != null ? date.toString() : "");
        });
        
        // 操作列 - 添加"续单"、"完成"、"租号"按钮
        actionCol.setCellFactory(col -> new javafx.scene.control.TableCell<Order, Void>() {
            private final Button renewBtn = new Button("续单");
            private final Button finishBtn = new Button("完成");
            private final Button rentBtn = new Button("租号");
            
            {
                renewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 3; -fx-font-size: 11;");
                renewBtn.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    handleRenewOrder(order);
                });
                
                finishBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 3; -fx-font-size: 11;");
                finishBtn.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    System.out.println("完成工单: " + order.id().getValue());
                    showInfo("工单已完成: " + order.id().getValue());   
                });
                
                rentBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 3; -fx-font-size: 11;");
                rentBtn.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    System.out.println("需要租号: " + order.id().getValue());
                    showInfo("租号成功: " + order.id().getValue());
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // 将三个按钮放在 HBox 中显示
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    hbox.getChildren().addAll(renewBtn, finishBtn, rentBtn);
                    setGraphic(hbox);
                }
            }
        });

        
        System.out.println("========== setupTableColumns 完成 ==========\n");
    }
    
    /**
     * 获取状态的中文标签
     */
    private String getStatusLabel(javafx_demo.utils.OrderStatusEnum status) {
        return switch (status) {
            case PENDING -> "等待客服确认";
            case CONFIRMED -> "已确认";
            case PROCESSING -> "进行中";
            case FAILURE -> "炸单";
            case COMPLETED -> "已完成";
            default -> "未知";
        };
    }
    
    /**
     * 处理续单操作
     */
    private void handleRenewOrder(Order order) {
        System.out.println("续单工单: " + order.id().getValue());
        showInfo("续单成功: " + order.id().getValue());
        // TODO: 实现续单业务逻辑
    }

    /**
     * 从缓存加载工单数据到表格（登录时已加载）
     */
    private void loadOrders() {
        try {
            System.out.println("\n========== MainController loadOrders 开始 ==========");
            // 从缓存获取工单数据（登录时已加载）
            ordersList = OrderJsonLoader.getCachedOrders();
            System.out.println("✅ 从缓存获取工单数据: " + ordersList.size() + " 条");
            
            ordersTable.setItems(ordersList);
            System.out.println("✅ 工单数据已绑定到表格");
            System.out.println("========== loadOrders 完成 ==========\n");
            
        } catch (Exception e) {
            System.err.println("❌ 加载工单数据失败: " + e.getMessage());
            e.printStackTrace();
            showError("加载工单数据失败: " + e.getMessage());
        }
    }

    /**
     * 简单的 JSON 解析方法（不依赖第三方库）
     */
    private void parseJsonOrders(String jsonContent) {
        try {
            // 提取 orders 数组内容
            int ordersStart = jsonContent.indexOf("\"orders\":");
            if (ordersStart == -1) return;
            
            int arrayStart = jsonContent.indexOf("[", ordersStart);
            int arrayEnd = jsonContent.lastIndexOf("]");
            if (arrayStart == -1 || arrayEnd == -1) return;
            
            String ordersArray = jsonContent.substring(arrayStart + 1, arrayEnd);
            
            // 按对象分割 - 更精确的方法
            int depth = 0;
            int start = 0;
            StringBuilder currentObject = new StringBuilder();
            
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
                        parseAndAddOrder(orderObj);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("JSON 解析错误: " + e.getMessage());
        }
    }
    
    /**
     * 解析单个工单对象并添加到列表
     */
    private void parseAndAddOrder(String orderObj) {
        try {
            String orderId = extractJsonValue(orderObj, "orderId");
            
            // 由于Order现在是JavaFX Property的record，这里我们只添加orderId到列表
            // 实际应用中可以根据需要扩展
            System.out.println("加载工单: " + orderId);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析工单对象失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 字符串中提取字段值
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return "";
        
        int valueStart = json.indexOf(":", keyIndex) + 1;
        int valueEnd = json.indexOf(",", valueStart);
        if (valueEnd == -1) valueEnd = json.length();
        
        String value = json.substring(valueStart, valueEnd).trim();
        // 移除引号和多余空格
        value = value.replaceAll("^\"|\"$", "").trim();
        
        // 如果是空字符串，对于数字类型返回 "0"
        if (value.isEmpty() && key.equals("amount")) {
            return "0";
        }
        
        return value;
    }

    /**
     * 设置当前用户信息
     */
    public void setUserInfo(String username) {
        this.currentUsername = username;
        usernameLabel.setText(username);
    }

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        statusLabel.setText("工单列表");
        System.out.println("显示工单");
    }

    @FXML
    private void showUserInfo() {
        setActiveButton(userInfoBtn);
        statusLabel.setText("个人信息");
        System.out.println("显示个人信息");
        showInfo("个人信息功能开发中...");
    }

    @FXML
    private void showDataManage() {
        setActiveButton(dataManageBtn);
        statusLabel.setText("工单管理");
        System.out.println("打开工单管理页面");
        SceneManager.getInstance().switchToOrderForm();
    }

    @FXML
    private void showSettings() {
        setActiveButton(settingsBtn);
        statusLabel.setText("设置");
        System.out.println("显示设置");
        showInfo("设置功能开发中...");
    }

    /**
     * 挂起选中的工单
     */
    @FXML
    private void handleSuspend() {
        System.out.println("挂起工单");
        showStatusAlert("挂起", "🔴 操作成功", "#e74c3c");
        updateStatusDisplay("操作: 挂起");
    }

    /**
     * 将选中的工单标记为就绪
     */
    @FXML
    private void handleReady() {
        System.out.println("就绪工单");
        showStatusAlert("就绪", "🟢 操作成功", "#2ecc71");
        updateStatusDisplay("操作: 就绪");
    }

    /**
     * 刷新工单列表
     */
    @FXML
    private void handleRefresh() {
        System.out.println("刷新工单列表");
        loadOrders();
        showInfo("工单列表已刷新");
        updateStatusDisplay("工单列表已刷新");
    }

    /**
     * 更新状态显示
     */
    private void updateStatusDisplay(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    /**
     * 显示状态提示
     */
    private void showStatusAlert(String title, String message, String color) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认退出");
        alert.setHeaderText(null);
        alert.setContentText("确定要退出登录吗?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("用户退出登录");
                SceneManager.getInstance().switchToLogin();
            }
        });
    }

    /**
     * 设置激活的菜单按钮样式
     */
    private void setActiveButton(Button activeButton) {
        for (Button btn : menuButtons) {
            if (btn == activeButton) {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-padding: 10;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c3e50; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-padding: 10;");
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("警告");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
