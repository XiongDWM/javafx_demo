package javafx_demo.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx_demo.entity.Order;
import javafx_demo.service.ApiService;
import javafx_demo.service.SseClient;
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
    @FXML private Button hangingBtn;

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
        // 启动 SSE 监听
        startSSE();
    }

    /** 启动 SSE 并注册事件回调 */
    private void startSSE() {
        SseClient sse = SseClient.getInstance();
        // 监听订单事件 — 按 resourceId 增量更新
        sse.on("ORDER", (domain, action, resourceId) -> {
            switch (action) {
                case "UPDATE" -> patchOrder(resourceId);
                case "DELETE" -> ordersList.removeIf(o -> resourceId.equals(o.getOrderId()));
                case "CREATE" -> loadOrders(); // 新建需要重新拉列表
            }
        });
        sse.connect(java.util.List.of("ORDER"));
    }

    /** 增量更新单条订单 */
    private void patchOrder(String orderId) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return ApiService.getOrderDetail(orderId);
            }
        };
        task.setOnSucceeded(e -> {
            Map<String, Object> detail = task.getValue();
            if (detail == null) return;
            Order updated = Order.fromMap(detail);
            for (int i = 0; i < ordersList.size(); i++) {
                if (orderId.equals(ordersList.get(i).getOrderId())) {
                    ordersList.set(i, updated);
                    return;
                }
            }
        });
        task.setOnFailed(e -> System.err.println("增量更新失败: " + task.getException().getMessage()));
        runAsync(task);
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

        // 操作列: 接单 / 续单 / 结束
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn = createBtn("接单", "#27ae60");
            private final Button renewBtn = createBtn("续单", "#3498db");
            private final Button closeBtn = createBtn("结束", "#e74c3c");

            {
                acceptBtn.setOnAction(e -> handleAcceptOrderInRow(getTableView().getItems().get(getIndex())));
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
                // 待接单 → 接单按钮
                if ("PENDING".equals(st) || "THIRD_PARTY_WAITING".equals(st)) {
                    box.getChildren().add(acceptBtn);
                }
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

    /** 接单 — 弹出输入工单号 + 图片上传弹窗 */
    @FXML
    private void handleAcceptOrder() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要开始接单吗？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("接单");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            SessionContext ctx = SessionContext.getInstance();
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiService.changeStatus(ctx.getUserId(), "ACTIVE");
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("已就绪");
                statusLabel.setText("就绪");
            });
            task.setOnFailed(e -> showError("操作失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    /** 行内接单按钮 — 直接弹图片上传弹窗 */
    private void handleAcceptOrderInRow(Order order) {
        showUploadDialogAndAccept(order.getOrderId());
    }

    /** 接单公共逻辑: 弹窗上传图片 → 预览 → 调用接单接口(picStart) */
    private void showUploadDialogAndAccept(String orderId) {
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("接单 - 上传开始截图");
        dialog.setHeaderText("工单: " + orderId + "\n请上传开始截图");

        ImageView preview = new ImageView();
        preview.setFitWidth(300);
        preview.setFitHeight(200);
        preview.setPreserveRatio(true);
        preview.setStyle("-fx-border-color: #ddd;");

        Label fileLabel = new Label("未选择文件");
        fileLabel.setStyle("-fx-text-fill: #7f8c8d;");

        final File[] selectedFile = {null};
        Button pickBtn = new Button("选择图片");
        pickBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 15;");
        pickBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择截图");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) {
                selectedFile[0] = f;
                fileLabel.setText(f.getName());
                preview.setImage(new Image(f.toURI().toString(), 300, 200, true, true));
            }
        });

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(24, 24);
        loading.setVisible(false);
        Label loadingLabel = new Label("上传中...");
        loadingLabel.setVisible(false);
        HBox loadingBox = new HBox(8, loading, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10, new HBox(10, pickBtn, fileLabel), preview, loadingBox);
        vb.setPadding(new Insets(15));
        vb.setAlignment(Pos.CENTER);
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(400);

        ButtonType submitType = new ButtonType("确认接单", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null); // 手动控制关闭时机

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            SessionContext ctx = SessionContext.getInstance();
            statusLabel.setText("接单中...");
            submitBtn.setDisable(true);
            pickBtn.setDisable(true);
            loading.setVisible(true);
            loadingLabel.setVisible(true);

            File file = selectedFile[0];
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String picStart = "";
                    if (file != null) {
                        picStart = ApiService.uploadImage(file);
                    }
                    ApiService.acceptOrder(ctx.getUserId(), orderId, picStart);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("接单成功: " + orderId);
                loadOrders();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                pickBtn.setDisable(false);
                loading.setVisible(false);
                loadingLabel.setVisible(false);
                showError("接单失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        dialog.showAndWait();
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

    /** 挂起 */
    @FXML
    private void handleHanging() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要挂起吗？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("挂起");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            SessionContext ctx = SessionContext.getInstance();
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiService.changeStatus(ctx.getUserId(), "HANGING");
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("已挂起");
                statusLabel.setText("挂起");
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
        final Button[] pickBtnRef = {null};
        if (order.isSecondHand()) {
            ImageView preview = new ImageView();
            preview.setFitWidth(250);
            preview.setFitHeight(160);
            preview.setPreserveRatio(true);

            Label fileLabel = new Label("未选择");
            fileLabel.setStyle("-fx-text-fill: #7f8c8d;");

            Button pickBtn = new Button("选择附加截图");
            pickBtnRef[0] = pickBtn;
            pickBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
            pickBtn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("选择图片");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.webp"));
                File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
                if (f != null) {
                    attachedFile[0] = f;
                    fileLabel.setText(f.getName());
                    preview.setImage(new Image(f.toURI().toString(), 250, 160, true, true));
                }
            });
            vb.getChildren().addAll(
                    new Separator(),
                    new Label("附加截图(二手单必填):"),
                    new HBox(10, pickBtn, fileLabel),
                    preview);
        }
        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(24, 24);
        loading.setVisible(false);
        Label loadingLabel = new Label("上传中...");
        loadingLabel.setVisible(false);
        HBox loadingBox = new HBox(8, loading, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        vb.getChildren().add(loadingBox);
        vb.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(400);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null); // 手动控制关闭时机

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            double price, amount;
            try {
                price = Double.parseDouble(priceField.getText());
                amount = Double.parseDouble(amountField.getText());
            } catch (NumberFormatException ex) {
                showError("请输入有效数字");
                return;
            }
            String unitType = unitBox.getValue();
            File fileToUpload = attachedFile[0];

            // 二手单必须上传图片
            if (order.isSecondHand() && fileToUpload == null) {
                showError("二手单续单必须上传附加截图");
                return;
            }

            statusLabel.setText("续单中...");
            submitBtn.setDisable(true);
            if (pickBtnRef[0] != null) {
                pickBtnRef[0].setDisable(true);
            }
            loading.setVisible(true);
            loadingLabel.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    String additionalPic = null;
                    if (fileToUpload != null) {
                        additionalPic = ApiService.uploadImage(fileToUpload);
                    }
                    ApiService.continueOrder(order.getOrderId(), price, amount, unitType, additionalPic);
                    // 二手单上传图片后更新状态为 THIRD_PARTY_TAKEN_PROCESS_DONE
                    if (order.isSecondHand() && additionalPic != null) {
                        ApiService.updateSecondHandStatus(order.getOrderId(), "THIRD_PARTY_TAKEN_PROCESS_DONE");
                    }
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("续单成功");
                loadOrders();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                if (pickBtnRef[0] != null) {
                    pickBtnRef[0].setDisable(false);
                }
                loading.setVisible(false);
                loadingLabel.setVisible(false);
                showError("续单失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        dialog.showAndWait();
    }

    /** 结束工单 — 必须上传截图(带预览弹窗) */
    private void handleCloseOrder(Order order) {
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("结束工单");
        dialog.setHeaderText("请上传结束截图 — 工单: " + order.getOrderId());

        ImageView preview = new ImageView();
        preview.setFitWidth(300);
        preview.setFitHeight(200);
        preview.setPreserveRatio(true);

        Label fileLabel = new Label("未选择");
        fileLabel.setStyle("-fx-text-fill: #7f8c8d;");

        final File[] selected = {null};
        Button pickBtn = new Button("选择截图");
        pickBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
        pickBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择结束截图");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) {
                selected[0] = f;
                fileLabel.setText(f.getName());
                preview.setImage(new Image(f.toURI().toString(), 300, 200, true, true));
            }
        });

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(24, 24);
        loading.setVisible(false);
        Label loadingLabel = new Label("上传中...");
        loadingLabel.setVisible(false);
        HBox loadingBox = new HBox(8, loading, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10,
            new HBox(10, pickBtn, fileLabel),
            preview,
            loadingBox);
        vb.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(400);

        ButtonType submitType = new ButtonType("确认结束", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null); // 手动控制关闭时机

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            File file = selected[0];
            if (file == null) {
                showError("结束工单需要上传截图");
                return;
            }
            statusLabel.setText("结束工单中...");
            submitBtn.setDisable(true);
            pickBtn.setDisable(true);
            loading.setVisible(true);
            loadingLabel.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    String picId = ApiService.uploadImage(file);
                    ApiService.closeOrder(order.getOrderId(), picId);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("工单已完成: " + order.getOrderId());
                loadOrders();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                pickBtn.setDisable(false);
                loading.setVisible(false);
                loadingLabel.setVisible(false);
                showError("关闭工单失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        dialog.showAndWait();
    }

    // ====================== 退出登录 ======================

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要退出登录吗?", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("确认退出");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SseClient.getInstance().disconnect();
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
