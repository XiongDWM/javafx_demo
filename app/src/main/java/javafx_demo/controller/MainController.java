package javafx_demo.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx_demo.entity.Order;
import javafx_demo.service.ApiService;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.SceneManager;
import javafx_demo.utils.SessionContext;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主控制器 — StackPane 多视图切换 + 调用真实后端 API
 */
public class MainController {

    // ---- Header ----
    @FXML private Label usernameLabel;
    @FXML private Button logoutButton;
    @FXML private Button acceptOrderBtn;
    @FXML private Button findingRequestBtn;
    @FXML private Button offlineBtn;

    // ---- Left nav ----
    @FXML private Button dashboardBtn;
    @FXML private Button dataManageBtn;
    @FXML private Button settingsBtn;

    // ---- Center StackPane ----
    @FXML private StackPane contentStack;

    // -- 工单视图 --
    @FXML private VBox orderPane;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> idCol;
    @FXML private TableColumn<Order, String> typeCol;
    @FXML private TableColumn<Order, String> customerCol;
    @FXML private TableColumn<Order, String> statusCol;
    @FXML private TableColumn<Order, String> amountCol;
    @FXML private TableColumn<Order, String> unitCol;
    @FXML private TableColumn<Order, String> incomeCol;
    @FXML private TableColumn<Order, String> issueDateCol;
    @FXML private TableColumn<Order, Void> actionCol;
    @FXML private Button refreshBtn;

    // -- 统计视图 --
    @FXML private VBox statsPane;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalIncomeLabel;

    // -- 设置视图 --
    @FXML private VBox settingsPane;

    // ---- Bottom ----
    @FXML private Label statusLabel;
    @FXML private Label versionLabel;

    private List<Button> menuButtons;
    private ObservableList<Order> ordersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ConfigManager config = ConfigManager.getInstance();
        versionLabel.setText("Version " + config.getAppVersion());

        menuButtons = Arrays.asList(dashboardBtn, dataManageBtn, settingsBtn);
        setupTableColumns();
        ordersTable.setItems(ordersList);

        // 默认显示工单列表
        showDashboard();
        // 异步加载今日工单
        loadOrders();
    }

    // ====================== 视图切换 (StackPane) ======================

    private void showOnly(Node target) {
        for (Node child : contentStack.getChildren()) {
            child.setVisible(child == target);
        }
    }

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        showOnly(orderPane);
        statusLabel.setText("工单列表");
    }

    @FXML
    private void showStatistics() {
        setActiveButton(dataManageBtn);
        showOnly(statsPane);
        statusLabel.setText("统计");
        loadStatistics();
    }

    @FXML
    private void showSettings() {
        setActiveButton(settingsBtn);
        showOnly(settingsPane);
        statusLabel.setText("设置");
    }

    // ====================== 表格列绑定 ======================

    private void setupTableColumns() {
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrderId()));
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTypeText()));
        customerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCustomer()));
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatusText()));
        amountCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getAmount())));
        unitCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUnitTypeText()));
        incomeCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getLowIncome())));
        issueDateCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getIssueDate()));

        // 操作列: 续单 / 结束
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button renewBtn = createBtn("续单", "#3498db");
            private final Button closeBtn = createBtn("结束", "#2ecc71");

            {
                renewBtn.setOnAction(e -> handleContinueOrder(getTableView().getItems().get(getIndex())));
                closeBtn.setOnAction(e -> handleCloseOrder(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Order order = getTableView().getItems().get(getIndex());
                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER);
                String st = order.getStatus();
                // 进行中的工单可以续单/结束
                if ("IN_PROGRESS".equals(st) || "THIRD_PARTY_TAKEN".equals(st)) {
                    box.getChildren().addAll(renewBtn, closeBtn);
                }
                setGraphic(box);
            }
        });
    }

    private Button createBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-cursor: hand; "
                + "-fx-padding: 4 12; -fx-background-radius: 3; -fx-font-size: 11;");
        return b;
    }

    // ====================== 数据加载 ======================

    @FXML
    private void handleRefresh() {
        loadOrders();
    }

    private void loadOrders() {
        statusLabel.setText("加载中...");
        SessionContext ctx = SessionContext.getInstance();
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiService.getTodayOrders(ctx.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            List<Order> orders = task.getValue().stream()
                    .map(Order::fromMap).collect(Collectors.toList());
            ordersList.setAll(orders);
            statusLabel.setText("共 " + orders.size() + " 条工单");
        });
        task.setOnFailed(e -> {
            statusLabel.setText("加载失败");
            showError("加载工单失败: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });
        runAsync(task);
    }

    private void loadStatistics() {
        totalOrdersLabel.setText("...");
        totalIncomeLabel.setText("...");
        SessionContext ctx = SessionContext.getInstance();
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return ApiService.getUserSummary(ctx.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            Map<String, Object> d = task.getValue();
            totalOrdersLabel.setText(String.valueOf(((Number) d.get("totalOrders")).intValue()));
            totalIncomeLabel.setText(String.format("%.2f", ((Number) d.get("totalIncome")).doubleValue()));
        });
        task.setOnFailed(e -> {
            totalOrdersLabel.setText("--");
            totalIncomeLabel.setText("--");
            showError("加载统计失败: " + task.getException().getMessage());
        });
        runAsync(task);
    }

    // ====================== 顶部按钮操作 ======================

    /** 接单 — 弹出输入工单号对话框 */
    @FXML
    private void handleAcceptOrder() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("接单");
        dlg.setHeaderText("请输入要接的工单号");
        dlg.setContentText("工单ID:");
        dlg.showAndWait().ifPresent(orderId -> {
            if (orderId.isBlank()) return;
            SessionContext ctx = SessionContext.getInstance();
            statusLabel.setText("接单中...");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiService.acceptOrder(ctx.getUserId(), orderId.trim(), "");
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("接单成功: " + orderId);
                loadOrders();
            });
            task.setOnFailed(e -> showError("接单失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    /** 离线 */
    @FXML
    private void handleOffline() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要设为离线状态吗？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("离线");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            SessionContext ctx = SessionContext.getInstance();
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiService.changeStatus(ctx.getUserId(), "OFFLINE");
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("已设为离线");
                statusLabel.setText("离线");
            });
            task.setOnFailed(e -> showError("操作失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    /** 创建找单请求 */
    @FXML
    private void handleFindingRequest() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("创建找单请求");
        dialog.setHeaderText("填写找单信息");

        // 表单
        ToggleGroup genderGroup = new ToggleGroup();
        RadioButton manBtn = new RadioButton("男单");
        manBtn.setToggleGroup(genderGroup);
        manBtn.setSelected(true);
        RadioButton womanBtn = new RadioButton("女单/Ai");
        womanBtn.setToggleGroup(genderGroup);
        TextArea descField = new TextArea();
        descField.setPromptText("备注（可选）");
        descField.setPrefRowCount(3);

        VBox vb = new VBox(10, new HBox(15, manBtn, womanBtn), descField);
        vb.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vb);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == submitType) {
                Map<String, Object> r = new HashMap<>();
                r.put("man", manBtn.isSelected());
                r.put("description", descField.getText());
                return r;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            SessionContext ctx = SessionContext.getInstance();
            boolean man = (boolean) data.get("man");
            String desc = (String) data.get("description");
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ApiService.submitFindingRequest(ctx.getUserId(), man, desc);
                    return null;
                }
            };
            task.setOnSucceeded(e -> showInfo("找单请求已提交"));
            task.setOnFailed(e -> showError("提交失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    // ====================== 工单行操作 ======================

    /** 续单 */
    private void handleContinueOrder(Order order) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("续单");
        dialog.setHeaderText("工单: " + order.getOrderId());

        TextField priceField = new TextField();
        priceField.setPromptText("单价");
        TextField amountField = new TextField();
        amountField.setPromptText("数量");
        ChoiceBox<String> unitBox = new ChoiceBox<>();
        unitBox.getItems().addAll("HOUR", "BATTLE", "DAY");
        unitBox.setValue(order.getUnitType() != null ? order.getUnitType() : "HOUR");

        VBox vb = new VBox(10,
                new Label("单价:"), priceField,
                new Label("数量:"), amountField,
                new Label("单位:"), unitBox);

        // 二手单需要上传附加截图
        final File[] attachedFile = {null};
        if (order.isSecondHand()) {
            Button pickBtn = new Button("选择附加截图");
            Label fileLabel = new Label("未选择");
            pickBtn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("选择图片");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.webp"));
                File f = fc.showOpenDialog(contentStack.getScene().getWindow());
                if (f != null) { attachedFile[0] = f; fileLabel.setText(f.getName()); }
            });
            vb.getChildren().addAll(new Label("附加截图(二手单):"), new HBox(10, pickBtn, fileLabel));
        }
        vb.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vb);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == submitType) {
                Map<String, Object> r = new HashMap<>();
                r.put("price", priceField.getText());
                r.put("amount", amountField.getText());
                r.put("unitType", unitBox.getValue());
                r.put("file", attachedFile[0]);
                return r;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            double price, amount;
            try {
                price = Double.parseDouble((String) data.get("price"));
                amount = Double.parseDouble((String) data.get("amount"));
            } catch (NumberFormatException ex) {
                showError("请输入有效数字");
                return;
            }
            String unitType = (String) data.get("unitType");
            File fileToUpload = (File) data.get("file");
            statusLabel.setText("续单中...");

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    String additionalPic = null;
                    if (fileToUpload != null) {
                        additionalPic = ApiService.uploadImage(fileToUpload);
                    }
                    ApiService.continueOrder(order.getOrderId(), price, amount, unitType, additionalPic);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("续单成功");
                loadOrders();
            });
            task.setOnFailed(e -> showError("续单失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    /** 结束工单 — 必须上传截图 */
    private void handleCloseOrder(Order order) {
        FileChooser fc = new FileChooser();
        fc.setTitle("选择结束截图（必需）");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(contentStack.getScene().getWindow());
        if (file == null) {
            showError("结束工单需要上传截图");
            return;
        }
        statusLabel.setText("结束工单中...");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String picId = ApiService.uploadImage(file);
                ApiService.closeOrder(order.getOrderId(), picId);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            showInfo("工单已完成: " + order.getOrderId());
            loadOrders();
        });
        task.setOnFailed(e -> showError("关闭工单失败: " + task.getException().getMessage()));
        runAsync(task);
    }

    // ====================== 退出登录 ======================

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要退出登录吗?", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("确认退出");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionContext.getInstance().clear();
                SceneManager.getInstance().switchToLogin();
            }
        });
    }

    // ====================== 工具 ======================

    public void setUserInfo(String username) {
        usernameLabel.setText(username);
    }

    private void setActiveButton(Button active) {
        for (Button btn : menuButtons) {
            if (btn == active) {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-padding: 10;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c3e50; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-padding: 10;");
            }
        }
    }

    private void runAsync(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showInfo(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("提示");
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("错误");
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}
