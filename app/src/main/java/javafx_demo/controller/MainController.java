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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx_demo.entity.Order;
import javafx_demo.entity.BookOrder;
import javafx_demo.entity.FindingRequest;
import javafx_demo.entity.LeaveRecord;
import javafx_demo.entity.SalaryAdvance;
import javafx_demo.entity.MaintenanceRecord;
import javafx_demo.service.ApiService;
import javafx_demo.service.HttpService;
import javafx_demo.service.SseClient;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.GameTypeStore;
import javafx_demo.utils.SceneManager;
import javafx_demo.utils.ScreenCaptureTool;
import javafx_demo.utils.SessionContext;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 主控制器 — StackPane 多视图切换 + 调用真实后端 API
 */
public class MainController {

    /** 空闲超时（毫秒）— 1.5 小时 */
    private static final long IDLE_TIMEOUT_MS = (long) (1.5 * 60 * 60 * 1000L);
    /** 空闲检查间隔（毫秒）— 1 分钟 */
    private static final long IDLE_CHECK_INTERVAL_MS = 60_000L;

    private volatile long lastActivityTime = System.currentTimeMillis();
    private java.util.Timer idleTimer;

    // ---- Header ----
    @FXML private Label usernameLabel;
    @FXML private Label userStatusLabel;
    @FXML private Button logoutButton;
    @FXML private Button acceptOrderBtn;
    @FXML private Button findingRequestBtn;
    @FXML private Button offlineBtn;
    @FXML private Button hangingBtn;

    // ---- Left nav ----
    @FXML private Button dashboardBtn;
    @FXML private Button bookOrderBtn;
    @FXML private Button dataManageBtn;
    @FXML private Button settingsBtn;

    // ---- Center StackPane ----
    @FXML private StackPane contentStack;

    // -- 工单视图 --
    @FXML private VBox orderPane;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> idCol;
    @FXML private TableColumn<Order, String> typeCol;
    @FXML private TableColumn<Order, String> gameTypeCol;
    @FXML private TableColumn<Order, String> rankCol;
    @FXML private TableColumn<Order, String> customerCol;
    @FXML private TableColumn<Order, String> statusCol;
    @FXML private TableColumn<Order, String> amountCol;
    @FXML private TableColumn<Order, String> unitCol;
    @FXML private TableColumn<Order, String> incomeCol;
    @FXML private TableColumn<Order, String> issueDateCol;
    @FXML private TableColumn<Order, Void> actionCol;
    @FXML private Button refreshBtn;

    // -- 找单请求 --
    @FXML private TableView<FindingRequest> findingTable;
    @FXML private TableColumn<FindingRequest, String> findingManCol;
    @FXML private TableColumn<FindingRequest, String> findingDescCol;
    @FXML private TableColumn<FindingRequest, String> findingTimeCol;
    @FXML private TableColumn<FindingRequest, String> findingStatusCol;
    @FXML private TableColumn<FindingRequest, Void> findingActionCol;
    @FXML private Button refreshFindingBtn;

    // -- 存单视图 --
    @FXML private VBox bookOrderPane;
    @FXML private TableView<BookOrder> bookOrdersTable;
    @FXML private TableColumn<BookOrder, String> boCustomerCol;
    @FXML private TableColumn<BookOrder, String> boCustomerIdCol;
    @FXML private TableColumn<BookOrder, String> boDetailsCol;
    @FXML private TableColumn<BookOrder, String> boAmountCol;
    @FXML private TableColumn<BookOrder, String> boRemainingCol;
    @FXML private TableColumn<BookOrder, String> boPriceCol;
    @FXML private TableColumn<BookOrder, String> boPicCol;
    @FXML private TableColumn<BookOrder, Void> boActionCol;
    @FXML private TableColumn<BookOrder, String> boCreateTimeCol;
    @FXML private Button addBookOrderBtn;
    @FXML private Button refreshBookOrderBtn;
    @FXML private Button boPrevPageBtn;
    @FXML private Button boNextPageBtn;
    @FXML private Label boPageLabel;

    // -- 统计视图 --
    @FXML private VBox statsPane;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalIncomeLabel;

    // -- 我的视图 --
    @FXML private ScrollPane settingsPane;

    // -- 请假记录 --
    @FXML private TableView<LeaveRecord> leaveTable;
    @FXML private TableColumn<LeaveRecord, String> leaveTypeCol;
    @FXML private TableColumn<LeaveRecord, String> leaveApplyTimeCol;
    @FXML private TableColumn<LeaveRecord, String> leaveEndTimeCol;
    @FXML private TableColumn<LeaveRecord, String> leaveStatusCol;
    @FXML private Button addLeaveBtn;
    @FXML private Button refreshLeaveBtn;
    @FXML private Button leavePrevBtn;
    @FXML private Button leaveNextBtn;
    @FXML private Label leavePageLabel;

    // -- 工资预支 --
    @FXML private TableView<SalaryAdvance> advanceTable;
    @FXML private TableColumn<SalaryAdvance, String> advanceNameCol;
    @FXML private TableColumn<SalaryAdvance, String> advanceAmountCol;
    @FXML private TableColumn<SalaryAdvance, String> advanceStatusCol;
    @FXML private Button addAdvanceBtn;
    @FXML private Button refreshAdvanceBtn;
    @FXML private Button advancePrevBtn;
    @FXML private Button advanceNextBtn;
    @FXML private Label advancePageLabel;

    // -- 维护记录 --
    @FXML private TableView<MaintenanceRecord> maintTable;
    @FXML private TableColumn<MaintenanceRecord, String> maintBossCol;
    @FXML private TableColumn<MaintenanceRecord, String> maintWechatCol;
    @FXML private TableColumn<MaintenanceRecord, String> maintTimeCol;
    @FXML private TableColumn<MaintenanceRecord, String> maintLogCol;
    @FXML private Button refreshMaintBtn;
    @FXML private Button maintPrevBtn;
    @FXML private Button maintNextBtn;
    @FXML private Label maintPageLabel;
    @FXML private VBox maintDetailPane;
    @FXML private Label maintDetailTitle;
    @FXML private ScrollPane maintDetailScroll;
    @FXML private Label maintDetailContent;
    @FXML private TextField maintInputField;
    @FXML private HBox maintInputActions;
    @FXML private Button maintConfirmBtn;
    @FXML private Button maintCancelBtn;
    @FXML private Button maintAddLogBtn;

    // ---- Bottom ----
    @FXML private Label statusLabel;
    @FXML private Label versionLabel;

    private List<Button> menuButtons;
    private ObservableList<Order> ordersList = FXCollections.observableArrayList();
    private ObservableList<FindingRequest> findingList = FXCollections.observableArrayList();
    private ObservableList<BookOrder> bookOrdersList = FXCollections.observableArrayList();
    private int boCurrentPage = 0;
    private int boTotalPages = 1;
    private static final int BO_PAGE_SIZE = 20;

    private ObservableList<LeaveRecord> leaveList = FXCollections.observableArrayList();
    private int leaveCurrentPage = 0;
    private int leaveTotalPages = 1;
    private static final int LEAVE_PAGE_SIZE = 10;

    private ObservableList<SalaryAdvance> advanceList = FXCollections.observableArrayList();
    private int advanceCurrentPage = 0;
    private int advanceTotalPages = 1;
    private static final int ADVANCE_PAGE_SIZE = 10;

    private ObservableList<MaintenanceRecord> maintList = FXCollections.observableArrayList();
    private int maintCurrentPage = 0;
    private int maintTotalPages = 1;
    private static final int MAINT_PAGE_SIZE = 10;
    private MaintenanceRecord selectedMaintRecord = null;

    @FXML
    public void initialize() {
        ConfigManager config = ConfigManager.getInstance();
        versionLabel.setText("Version " + config.getAppVersion());

        menuButtons = Arrays.asList(dashboardBtn, bookOrderBtn, dataManageBtn, settingsBtn);
        setupTableColumns();
        setupFindingTableColumns();
        setupBookOrderTableColumns();
        setupLeaveTableColumns();
        setupAdvanceTableColumns();
        setupMaintTableColumns();
        ordersTable.setItems(ordersList);
        findingTable.setItems(findingList);
        bookOrdersTable.setItems(bookOrdersList);
        leaveTable.setItems(leaveList);
        advanceTable.setItems(advanceList);
        maintTable.setItems(maintList);

        // 启用所有表格单元格级别选中与复制
        enableCellCopy(ordersTable);
        enableCellCopy(findingTable);
        enableCellCopy(bookOrdersTable);
        enableCellCopy(leaveTable);
        enableCellCopy(advanceTable);
        enableCellCopy(maintTable);

        // 默认显示工单列表
        showDashboard();
        // 异步加载今日工单
        loadOrders();
        // 启动 SSE 监听
        startSSE();
        // 启动空闲超时检测
        startIdleTimer();
    }

    /** 启动空闲超时检测：监听鼠标/键盘事件重置计时，定时检查是否超过 30 分钟 */
    private void startIdleTimer() {
        // 延迟到 Scene 准备好后注册事件过滤器
        Platform.runLater(() -> {
            var scene = contentStack.getScene();
            if (scene != null) {
                scene.addEventFilter(javafx.scene.input.InputEvent.ANY, e -> lastActivityTime = System.currentTimeMillis());
            }
        });

        idleTimer = new java.util.Timer("IdleChecker", true);
        idleTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastActivityTime > IDLE_TIMEOUT_MS) {
                    Platform.runLater(() -> forceLogout());
                }
            }
        }, IDLE_CHECK_INTERVAL_MS, IDLE_CHECK_INTERVAL_MS);
    }

    /** 超时强制登出 */
    private void forceLogout() {
        forceLogout(null);
    }

    /** 强制登出，可附带提示信息 */
    private void forceLogout(String message) {
        if (idleTimer != null) { idleTimer.cancel(); idleTimer = null; }
        // 先通知后端
        try { ApiService.logout(); } catch (Exception ignored) {}
        SseClient.getInstance().disconnect();
        SessionContext.getInstance().clear();
        SceneManager.getInstance().switchToLogin();
        if (message != null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("提示");
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        }
    }

    /** 启动 SSE 并注册事件回调 */
    private void startSSE() {
        SseClient sse = SseClient.getInstance();

        // 全局日志 — 方便调试所有事件
        sse.on("*", (domain, action, resourceId) ->
                System.out.println("[SSE] 收到事件: domain=" + domain + " action=" + action + " resourceId=" + resourceId));

        // 监听订单事件 — 按 action 增量/全量更新
        sse.on("ORDER", (domain, action, resourceId) -> {
            switch (action) {
                case "UPDATE" -> patchOrder(resourceId);
                case "DELETE" -> ordersList.removeIf(o -> resourceId.equals(o.getOrderId()));
                case "CREATE" -> loadOrders(); // 新建需要重新拉列表
            }
        });

        // 监听找单请求事件 — 直接刷新全量列表
        sse.on("FINDING_REQUEST", (domain, action, resourceId) -> {
            Platform.runLater(() -> loadFindingList());
        });

        // 监听存单事件 — 直接刷新列表
        sse.on("BOOKING", (domain, action, resourceId) -> {
            Platform.runLater(() -> loadBookOrders());
        });

        sse.connect(java.util.List.of("ORDER", "FINDING_REQUEST", "BOOKING"));
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
        loadFindingList();
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
        statusLabel.setText("我的");
        loadLeaveRecords();
        loadAdvanceRecords();
        loadMaintRecords();
    }

    @FXML
    private void showBookOrders() {
        setActiveButton(bookOrderBtn);
        showOnly(bookOrderPane);
        statusLabel.setText("存单列表");
        loadBookOrders();
    }

    // ====================== 单元格复制支持 ======================

    @SuppressWarnings("unchecked")
    private <S> void enableCellCopy(TableView<S> table) {
        // 开启单元格选择模式
        table.getSelectionModel().setCellSelectionEnabled(true);

        // 右键菜单
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        copyItem.setOnAction(e -> copySelectedCell(table));
        ContextMenu menu = new ContextMenu(copyItem);
        table.setContextMenu(menu);

        // Ctrl+C / Cmd+C 快捷键
        table.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.C) {
                copySelectedCell(table);
                e.consume();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <S> void copySelectedCell(TableView<S> table) {
        var pos = table.getSelectionModel().getSelectedCells().stream().findFirst().orElse(null);
        if (pos == null) return;
        int row = pos.getRow();
        TableColumn col = pos.getTableColumn();
        if (col == null || row < 0 || row >= table.getItems().size()) return;
        Object cellValue = col.getCellObservableValue(table.getItems().get(row));
        String text = "";
        if (cellValue instanceof javafx.beans.value.ObservableValue<?> obs) {
            Object val = obs.getValue();
            text = val == null ? "" : val.toString();
        }
        if (!text.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    // ====================== 表格列绑定 ======================

    private void setupTableColumns() {
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrderId()));
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTypeText()));
        gameTypeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getGameType() != null ? cd.getValue().getGameType() : ""));
        rankCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRankInfo() != null ? cd.getValue().getRankInfo() : ""));
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

    // ====================== 找单请求表格 ======================

    private void setupFindingTableColumns() {
        // 性别: null=不限, true=男单, false=女单
        findingManCol.setCellValueFactory(cd -> {
            Boolean man = cd.getValue().getMan();
            return new SimpleStringProperty(man == null ? "不限" : (man ? "男单" : "女单"));
        });
        findingManCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color;
                switch (item) {
                    case "男单" -> color = "-fx-text-fill: #2980b9;";
                    case "女单" -> color = "-fx-text-fill: #e84393;";
                    default -> color = "-fx-text-fill: #7f8c8d;";
                }
                setStyle("-fx-alignment: CENTER; " + color + " -fx-font-weight: bold;");
            }
        });

        findingDescCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDescription() != null ? cd.getValue().getDescription() : ""));

        // 请求时间: 只显示 HH:mm
        findingTimeCol.setCellValueFactory(cd -> {
            String raw = cd.getValue().getRequestedAt();
            if (raw == null || raw.isEmpty()) return new SimpleStringProperty("-");
            // 取时间部分 "HH:mm"
            if (raw.contains("T")) {
                String timePart = raw.substring(raw.indexOf("T") + 1);
                if (timePart.length() >= 5) return new SimpleStringProperty(timePart.substring(0, 5));
            }
            if (raw.contains(" ") && raw.length() > 11) {
                String timePart = raw.substring(raw.indexOf(" ") + 1);
                if (timePart.length() >= 5) return new SimpleStringProperty(timePart.substring(0, 5));
            }
            return new SimpleStringProperty(raw);
        });

        // 状态: null=撤销, true=已找到, false=寻找中
        findingStatusCol.setCellValueFactory(cd -> {
            Boolean f = cd.getValue().getFulfilled();
            return new SimpleStringProperty(f == null ? "撤销" : (f ? "已找到" : "寻找中"));
        });
        findingStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color;
                switch (item) {
                    case "已找到" -> color = "-fx-text-fill: #27ae60;";
                    case "寻找中" -> color = "-fx-text-fill: #e67e22;";
                    default -> color = "-fx-text-fill: #95a5a6;";
                }
                setStyle("-fx-alignment: CENTER; " + color + " -fx-font-weight: bold;");
            }
        });

        // 操作列: 撤销按钮
        findingActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = createBtn("撤销", "#e74c3c");
            {
                cancelBtn.setOnAction(e -> {
                    FindingRequest req = getTableView().getItems().get(getIndex());
                    handleCancelFinding(req);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                FindingRequest req = getTableView().getItems().get(getIndex());
                // 只有"寻找中"状态才显示撤销按钮
                if (req.getFulfilled() != null && !req.getFulfilled()) {
                    setGraphic(cancelBtn);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void handleCancelFinding(FindingRequest req) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要撤销此找单请求吗？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("撤销找单");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ApiService.cancelFindingRequest(req.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("已撤销");
                loadFindingList();
            });
            task.setOnFailed(e -> showError("撤销失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    private void loadFindingList() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiService.queryFindingList();
            }
        };
        task.setOnSucceeded(e -> {
            List<FindingRequest> items = task.getValue().stream()
                    .map(FindingRequest::fromMap).collect(Collectors.toList());
            findingList.setAll(items);
        });
        task.setOnFailed(e -> showError("加载找单请求失败: " + task.getException().getMessage()));
        runAsync(task);
    }

    @FXML
    private void handleRefreshFinding() { loadFindingList(); }

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
                updateUserStatus("ACTIVE");
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

        Consumer<File> captureCallback = file -> {
            if (file != null) {
                selectedFile[0] = file;
                fileLabel.setText("截图");
                preview.setImage(new Image(file.toURI().toString(), 300, 200, true, true));
            }
        };
        Button captureBtn = new Button("截图 (Ctrl+Alt+A)");
        captureBtn.setStyle("-fx-background-color: #19b33d; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
        captureBtn.setOnAction(e -> ScreenCaptureTool.capture(dialog.getDialogPane().getScene().getWindow(), captureCallback));
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.isControlDown() && ke.isAltDown() && ke.getCode() == KeyCode.A) {
                captureBtn.fire();
                ke.consume();
            }
        });

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(24, 24);
        loading.setVisible(false);
        Label loadingLabel = new Label("上传中...");
        loadingLabel.setVisible(false);
        HBox loadingBox = new HBox(8, loading, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10, new HBox(10, pickBtn, captureBtn, fileLabel), preview, loadingBox);
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
            if (selectedFile[0] == null) {
                showError("请先上传或截取开始截图");
                return;
            }
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
                    String picStart = ApiService.uploadImage(file);
                    ApiService.acceptOrder(ctx.getUserId(), orderId, picStart);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("接单成功: " + orderId);
                updateUserStatus("BUSY");
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

        ScreenCaptureTool.showFloatingTrigger(captureCallback);
        dialog.setOnHidden(e -> ScreenCaptureTool.hideFloatingTrigger());
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
                updateUserStatus("OFFLINE");
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
                updateUserStatus("HANGING");
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

        // 性别: 男单 / 女单 / 不限
        ToggleGroup genderGroup = new ToggleGroup();
        RadioButton manBtn = new RadioButton("男单");
        manBtn.setToggleGroup(genderGroup);
        manBtn.setSelected(true);
        RadioButton womanBtn = new RadioButton("女单/Ai");
        womanBtn.setToggleGroup(genderGroup);
        RadioButton anyBtn = new RadioButton("不限");
        anyBtn.setToggleGroup(genderGroup);

        // 游戏类型下拉 + 新增按钮
        ComboBox<String> gameTypeBox = new ComboBox<>();
        gameTypeBox.getItems().addAll(GameTypeStore.load());
        gameTypeBox.setPromptText("选择游戏");
        gameTypeBox.setPrefWidth(160);
        Button addGameBtn = new Button("+");
        addGameBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 10;");
        addGameBtn.setOnAction(e -> {
            TextInputDialog inputDlg = new TextInputDialog();
            inputDlg.setTitle("新增游戏");
            inputDlg.setHeaderText(null);
            inputDlg.setContentText("游戏名:");
            inputDlg.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isEmpty() && !gameTypeBox.getItems().contains(trimmed)) {
                    GameTypeStore.add(trimmed);
                    gameTypeBox.getItems().add(trimmed);
                }
                if (!trimmed.isEmpty()) gameTypeBox.setValue(trimmed);
            });
        });

        // 段位输入
        TextField rankField = new TextField();
        rankField.setPromptText("段位（如: 黄金、钻石）");

        // 备注
        // TextArea descField = new TextArea();
        // descField.setPromptText("备注（可选）");
        // descField.setPrefRowCount(3);

        VBox vb = new VBox(10,
                new Label("性别:"), new HBox(15, manBtn, womanBtn, anyBtn)
                ,new Label("游戏类型:"), new HBox(8, gameTypeBox, addGameBtn)
                ,new Label("段位:"), rankField
                // , new Label("备注:"), descField
            );
        vb.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(420);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == submitType) {
                Map<String, Object> r = new HashMap<>();
                // 不限 → null
                if (anyBtn.isSelected()) {
                    r.put("man", null);
                } else {
                    r.put("man", manBtn.isSelected());
                }
                // r.put("description", descField.getText());
                r.put("gameType", gameTypeBox.getValue());
                r.put("rank", rankField.getText());
                return r;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            SessionContext ctx = SessionContext.getInstance();
            Boolean man = (Boolean) data.get("man");
            // String desc = (String) data.get("description");
            String gameType = (String) data.get("gameType");
            String rank = (String) data.get("rank");
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ApiService.submitFindingRequest(ctx.getUserId(), man, gameType, rank);
                    ApiService.changeStatus(ctx.getUserId(), "ACTIVE");
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("找单请求已提交");
                updateUserStatus("PREPARE");
            });
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
        unitBox.getItems().addAll("HOUR", "BATTLE", "DAY"); //修改为中文显示，key值任然为英文
        unitBox.setValue(order.getUnitType() != null ? order.getUnitType() : "HOUR");

        VBox vb = new VBox(10,
                new Label("单价:"), priceField,
                new Label("数量:"), amountField,
                new Label("单位:"), unitBox);

        // 二手单需要上传附加截图
        final File[] attachedFile = {null};
        final Button[] pickBtnRef = {null};
        Consumer<File> floatingCb = null;
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
            Consumer<File> attachCaptureCb = file -> {
                if (file != null) {
                    attachedFile[0] = file;
                    fileLabel.setText("截图");
                    preview.setImage(new Image(file.toURI().toString(), 250, 160, true, true));
                }
            };
            Button captureBtn = new Button("截图 (Ctrl+Alt+A)");
            captureBtn.setStyle("-fx-background-color: #19b33d; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
            captureBtn.setOnAction(e -> ScreenCaptureTool.capture(dialog.getDialogPane().getScene().getWindow(), attachCaptureCb));
            dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
                if (ke.isControlDown() && ke.isAltDown() && ke.getCode() == KeyCode.A) {
                    captureBtn.fire();
                    ke.consume();
                }
            });
            vb.getChildren().addAll(
                    new Separator(),
                    new Label("附加结束截图(二手单必填):"),
                    new HBox(10, pickBtn, captureBtn, fileLabel),
                    preview);
            floatingCb = attachCaptureCb;
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
                    ApiService.continueOrder(order.getOrderId(), price, amount, unitType, additionalPic); // 后端接口自行处理二手单，无需区分方法调用
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("续单成功");
                updateUserStatus("BUSY");
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

        if (floatingCb != null) {
            ScreenCaptureTool.showFloatingTrigger(floatingCb);
        }
        dialog.setOnHidden(e -> ScreenCaptureTool.hideFloatingTrigger());
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

        Consumer<File> closeCaptureCallback = file -> {
            if (file != null) {
                selected[0] = file;
                fileLabel.setText("截图");
                preview.setImage(new Image(file.toURI().toString(), 300, 200, true, true));
            }
        };
        Button captureBtn = new Button("截图 (Ctrl+Alt+A)");
        captureBtn.setStyle("-fx-background-color: #19b33d; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
        captureBtn.setOnAction(e -> ScreenCaptureTool.capture(dialog.getDialogPane().getScene().getWindow(), closeCaptureCallback));
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.isControlDown() && ke.isAltDown() && ke.getCode() == KeyCode.A) {
                captureBtn.fire();
                ke.consume();
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
            new HBox(10, pickBtn, captureBtn, fileLabel),
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
                    updateUserStatus("ONLINE"); // 结束后回到在线状态
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

        ScreenCaptureTool.showFloatingTrigger(closeCaptureCallback);
        dialog.setOnHidden(e -> ScreenCaptureTool.hideFloatingTrigger());
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
                if (idleTimer != null) { idleTimer.cancel(); idleTimer = null; }
                try { ApiService.logout(); } catch (Exception ignored) {}
                SseClient.getInstance().disconnect();
                SessionContext.getInstance().clear();
                SceneManager.getInstance().switchToLogin();
            }
        });
    }

    // ====================== 工具 ======================

    public void setUserInfo(String username) {
        usernameLabel.setText(username);
        updateUserStatus("ONLINE");
    }

    private void updateUserStatus(String status) {
        String dot = "●";
        String text;
        String color;
        switch (status) {
            case "ONLINE"  -> { text = "在线";  color = "white"; }
            case "PREPARE" -> { text = "找单中"; color = "#00bcd4"; }
            case "ACTIVE"  -> { text = "就绪";  color = "#27ae60"; }
            case "HANGING" -> { text = "挂起";  color = "#e58e15"; }
            case "OFFLINE" -> { text = "离线";  color = "#95a5a6"; }
            case "BUSY" -> { text = "忙碌"; color = "#e74c3c"; }
            default        -> { text = status;  color = "#95a5a6"; }
        }
        userStatusLabel.setText(dot + " " + text);
        userStatusLabel.setStyle("-fx-text-fill: " + color + ";");
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
        // 包装 onFailed，优先拦截 401 UnauthorizedException
        var originalOnFailed = task.getOnFailed();
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (isUnauthorized(ex)) {
                Platform.runLater(() -> forceLogout("自动登出，请重新登录"));
                return;
            }
            // 404 静默忽略，不弹窗
            if (isNotFound(ex)) {
                System.err.println("[404] 接口不存在，已静默忽略");
                return;
            }
            if (originalOnFailed != null) {
                originalOnFailed.handle(e);
            }
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** 检查异常链中是否包含 UnauthorizedException */
    private static boolean isUnauthorized(Throwable ex) {
        while (ex != null) {
            if (ex instanceof HttpService.UnauthorizedException) return true;
            ex = ex.getCause();
        }
        return false;
    }

    /** 检查异常链中是否包含 NotFoundException (404) */
    private static boolean isNotFound(Throwable ex) {
        while (ex != null) {
            if (ex instanceof HttpService.NotFoundException) return true;
            ex = ex.getCause();
        }
        return false;
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

    // ====================== 存单表格列绑定 ======================

    private void setupBookOrderTableColumns() {
        boCustomerCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCustomer() != null ? cd.getValue().getCustomer() : ""));
        boCustomerIdCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCustomerId() != null ? cd.getValue().getCustomerId() : ""));
        boDetailsCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDetails() != null ? cd.getValue().getDetails() : ""));
        boAmountCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getAmount())));
        boRemainingCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getRemaining())));
        boRemainingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                BookOrder bo = getTableView().getItems().get(getIndex());
                double remaining = bo.getRemaining();
                double amount = bo.getAmount();
                int pct = amount > 0 ? (int) Math.round(remaining / amount * 100) : 0;

                String barColor;
                if (pct <= 0) barColor = "#bdc3c7";
                else if (pct <= 25) barColor = "#e74c3c";
                else if (pct <= 50) barColor = "#f1c40f";
                else if (pct <= 75) barColor = "#7ec87e";
                else barColor = "#27ae60";

                Label valueLabel = new Label(String.valueOf(remaining));
                Label pctLabel = new Label(pct + "%");
                pctLabel.setMinWidth(35);
                pctLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");

                double barWidth = 60;
                double fillWidth = Math.min(pct, 100) / 100.0 * barWidth;

                Region bgBar = new Region();
                bgBar.setPrefSize(barWidth, 6);
                bgBar.setMinSize(barWidth, 6);
                bgBar.setMaxSize(barWidth, 6);
                bgBar.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 3;");

                Region fillBar = new Region();
                fillBar.setPrefSize(fillWidth, 6);
                fillBar.setMinSize(fillWidth, 6);
                fillBar.setMaxSize(fillWidth, 6);
                fillBar.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 3;");

                StackPane barPane = new StackPane(bgBar, fillBar);
                StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);

                HBox box = new HBox(5, valueLabel, pctLabel, barPane);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        boPriceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getPrice())));
        boCreateTimeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCreateTime() != null ? cd.getValue().getCreateTime() : ""));

        // 图片列: 有值显示"查看"链接，无值显示"-"
        boPicCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("查看");
            {
                viewLink.setOnAction(e -> {
                    BookOrder bo = getTableView().getItems().get(getIndex());
                    showImagePreviewDialog(bo.getPicProvence());
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                BookOrder bo = getTableView().getItems().get(getIndex());
                String pic = bo.getPicProvence();
                if (pic != null && !pic.isBlank()) {
                    setGraphic(viewLink);
                } else {
                    setGraphic(new Label("-"));
                }
            }
        });

        // 操作列: 开始 / 关闭 / 上传图片 / 续存
        boActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button startBtn = createBtn("开始", "#27ae60");
            private final Button closeBtn = createBtn("关闭", "#e74c3c");
            private final Button uploadPicBtn = createBtn("上传图片", "#3498db");
            private final Button renewBtn = createBtn("续存", "#e58e15");

            {
                startBtn.setOnAction(e -> handleStartBookOrder(getTableView().getItems().get(getIndex())));
                closeBtn.setOnAction(e -> handleCloseBookOrder(getTableView().getItems().get(getIndex())));
                uploadPicBtn.setOnAction(e -> handleUploadBookOrderPic(getTableView().getItems().get(getIndex())));
                renewBtn.setOnAction(e -> handleRenewBookOrder(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(startBtn, closeBtn, uploadPicBtn, renewBtn);
                setGraphic(box);
            }
        });
    }

    // ====================== 存单数据加载 ======================

    @FXML
    private void handleRefreshBookOrders() {
        loadBookOrders();
    }

    private void loadBookOrders() {
        statusLabel.setText("加载存单中...");
        Task<ApiService.PageResult> task = new Task<>() {
            @Override
            protected ApiService.PageResult call() throws Exception {
                return ApiService.queryBookOrders(boCurrentPage, BO_PAGE_SIZE, null);
            }
        };
        task.setOnSucceeded(e -> {
            ApiService.PageResult pr = task.getValue();
            List<BookOrder> orders = pr.content.stream()
                    .map(BookOrder::fromMap).collect(Collectors.toList());
            bookOrdersList.setAll(orders);
            boTotalPages = Math.max(pr.totalPages, 1);
            boPageLabel.setText("第 " + (boCurrentPage + 1) + " 页 / 共 " + boTotalPages + " 页");
            boPrevPageBtn.setDisable(boCurrentPage <= 0);
            boNextPageBtn.setDisable(boCurrentPage >= boTotalPages - 1);
            statusLabel.setText("共 " + pr.totalElements + " 条存单");
        });
        task.setOnFailed(e -> {
            statusLabel.setText("加载存单失败");
            showError("加载存单失败: " + task.getException().getMessage());
        });
        runAsync(task);
    }

    @FXML
    private void handleBoPrevPage() {
        if (boCurrentPage > 0) {
            boCurrentPage--;
            loadBookOrders();
        }
    }

    @FXML
    private void handleBoNextPage() {
        if (boCurrentPage < boTotalPages - 1) {
            boCurrentPage++;
            loadBookOrders();
        }
    }

    // ====================== 存单操作 ======================

    /** 开始 — 从存单创建 order */
    private void handleStartBookOrder(BookOrder bo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要从此存单创建工单吗？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("开始存单");
        confirm.setHeaderText("客户: " + bo.getCustomer());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            statusLabel.setText("创建工单中...");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ApiService.startBookOrder(bo.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showInfo("工单已从存单创建");
                loadBookOrders();
            });
            task.setOnFailed(e -> showError("操作失败: " + task.getException().getMessage()));
            runAsync(task);
        });
    }

    /** 关闭存单 — TODO: 待后端接口实现 */
    private void handleCloseBookOrder(BookOrder bo) {
        // TODO: 调用关闭存单接口 /bookOrder/close
        showInfo("关闭存单功能开发中...");
    }

    /** 上传图片到存单 — TODO: 待后端接口实现 */
    private void handleUploadBookOrderPic(BookOrder bo) {
        // TODO: 调用上传图片到存单接口 /bookOrder/uploadPic
        showInfo("上传图片功能开发中...");
    }

    /** 续存 — 弹窗输入续单单数、总价、图片 */
    private void handleRenewBookOrder(BookOrder bo) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("续存");
        dialog.setHeaderText("客户: " + bo.getCustomer() + " | 微信号: " + (bo.getCustomerId() != null ? bo.getCustomerId() : ""));

        Spinner<Double> amountField = new Spinner<>(0, 999999, 0, 1);
        amountField.setEditable(true);
        amountField.setPrefWidth(200);
        Spinner<Double> priceField = new Spinner<>(0, 999999, 0, 0.01);
        priceField.setEditable(true);
        priceField.setPrefWidth(200);

        // 多图上传区域
        List<File> imageFiles = new ArrayList<>();
        List<String> uploadedImageIds = new ArrayList<>();
        FlowPane imagePane = new FlowPane(10, 10);
        imagePane.setPrefWrapLength(400);

        // 添加按钮（虚线框 + 号）
        Button addImageBtn = createAddImageButton();
        imagePane.getChildren().add(addImageBtn);

        addImageBtn.setOnAction(e -> showImagePickerPopup(dialog, imageFiles, uploadedImageIds, imagePane, addImageBtn));

        // 截图回调
        Consumer<File> renewCaptureCb = file -> {
            if (file != null) {
                addImageToPane(file, imageFiles, uploadedImageIds, imagePane, addImageBtn, dialog);
            }
        };
        // 截图快捷键
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.isControlDown() && ke.isAltDown() && ke.getCode() == KeyCode.A) {
                ScreenCaptureTool.capture(dialog.getDialogPane().getScene().getWindow(), renewCaptureCb);
                ke.consume();
            }
        });

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(24, 24);
        loading.setVisible(false);
        Label loadingLabel = new Label("提交中...");
        loadingLabel.setVisible(false);
        HBox loadingBox = new HBox(8, loading, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10,
                new Label("续单单数:"), amountField,
                new Label("总价:"), priceField,
                new Label("图片(可选):"), imagePane,
                loadingBox);
        vb.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(460);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            double amount;
            double price;
            try {
                amount = Double.parseDouble(amountField.getEditor().getText().trim());
            } catch (NumberFormatException ex) {
                showError("续单单数格式错误"); return;
            }
            try {
                price = Double.parseDouble(priceField.getEditor().getText().trim());
            } catch (NumberFormatException ex) {
                showError("总价格式错误"); return;
            }
            if (amount <= 0) { showError("续单单数必须大于0"); return; }
            if (price <= 0) { showError("总价必须大于0"); return; }

            submitBtn.setDisable(true);
            loading.setVisible(true);
            loadingLabel.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // 上传尚未上传的图片
                    for (int i = 0; i < imageFiles.size(); i++) {
                        if (i >= uploadedImageIds.size() || uploadedImageIds.get(i) == null) {
                            String id = ApiService.uploadImage(imageFiles.get(i));
                            if (i < uploadedImageIds.size()) {
                                uploadedImageIds.set(i, id);
                            } else {
                                uploadedImageIds.add(id);
                            }
                        }
                    }
                    String picProvence = uploadedImageIds.isEmpty() ? null : String.join(",", uploadedImageIds);
                    // TODO: 调用续存接口 /bookOrder/renew (amount, price, picProvence, bookOrderId=bo.getId())
                    // 目前先打印参数，待后端实现后替换
                    System.out.println("[续存] bookOrderId=" + bo.getId() + " amount=" + amount + " price=" + price + " pic=" + picProvence);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("续存提交成功");
                loadBookOrders();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                loading.setVisible(false);
                loadingLabel.setVisible(false);
                showError("续存失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        ScreenCaptureTool.showFloatingTrigger(renewCaptureCb);
        dialog.setOnHidden(e -> ScreenCaptureTool.hideFloatingTrigger());
        dialog.showAndWait();
    }

    // ====================== 新增存单 ======================

    @FXML
    private void handleAddBookOrder() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("新增存单");
        dialog.setHeaderText(null);

        TextField customerField = new TextField();
        customerField.setPromptText("客户称呼");
        TextField customerIdField = new TextField();
        customerIdField.setPromptText("微信号");
        TextArea detailsField = new TextArea();
        detailsField.setPromptText("备注额外信息（可选）");
        detailsField.setPrefRowCount(3);
        Spinner<Double> amountField = new Spinner<>(0, 999999, 0, 1);
        amountField.setEditable(true);
        amountField.setPrefWidth(200);
        Spinner<Double> priceField = new Spinner<>(0, 999999, 0, 0.01);
        priceField.setEditable(true);
        priceField.setPrefWidth(200);

        // 多图上传区域
        List<File> imageFiles = new ArrayList<>();
        List<String> uploadedImageIds = new ArrayList<>();
        FlowPane imagePane = new FlowPane(10, 10);
        imagePane.setPrefWrapLength(400);

        Button addImageBtn = createAddImageButton();
        imagePane.getChildren().add(addImageBtn);

        addImageBtn.setOnAction(e -> showImagePickerPopup(dialog, imageFiles, uploadedImageIds, imagePane, addImageBtn));

        // 截图回调
        Consumer<File> addBookCaptureCb = file -> {
            if (file != null) {
                addImageToPane(file, imageFiles, uploadedImageIds, imagePane, addImageBtn, dialog);
            }
        };
        // 截图快捷键
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.isControlDown() && ke.isAltDown() && ke.getCode() == KeyCode.A) {
                ScreenCaptureTool.capture(dialog.getDialogPane().getScene().getWindow(), addBookCaptureCb);
                ke.consume();
            }
        });

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(24, 24);
        loading.setVisible(false);
        Label loadingLabel = new Label("提交中...");
        loadingLabel.setVisible(false);
        HBox loadingBox = new HBox(8, loading, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10,
                new Label("客户称呼:"), customerField,
                new Label("微信号:"), customerIdField,
                new Label("存单数量:"), amountField,
                new Label("存单总价:"), priceField,
                new Label("备注:"), detailsField,
                new Label("图片(可选):"), imagePane,
                loadingBox);
        vb.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(460);

        ButtonType submitType = new ButtonType("新增", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            String customer = customerField.getText().trim();
            String customerId = customerIdField.getText().trim();
            if (customer.isEmpty()) { showError("请填写客户称呼"); return; }
            if (customerId.isEmpty()) { showError("请填写微信号"); return; }
            double amount;
            double price;
            try {
                amount = Double.parseDouble(amountField.getEditor().getText().trim());
            } catch (NumberFormatException ex) {
                showError("存单数量格式错误"); return;
            }
            try {
                price = Double.parseDouble(priceField.getEditor().getText().trim());
            } catch (NumberFormatException ex) {
                showError("存单总价格式错误"); return;
            }
            if (amount <= 0) { showError("存单数量必须大于0"); return; }
            if (price <= 0) { showError("存单总价必须大于0"); return; }
            String details = detailsField.getText().trim();

            submitBtn.setDisable(true);
            loading.setVisible(true);
            loadingLabel.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // 上传尚未上传的图片
                    for (int i = 0; i < imageFiles.size(); i++) {
                        if (i >= uploadedImageIds.size() || uploadedImageIds.get(i) == null) {
                            String id = ApiService.uploadImage(imageFiles.get(i));
                            if (i < uploadedImageIds.size()) {
                                uploadedImageIds.set(i, id);
                            } else {
                                uploadedImageIds.add(id);
                            }
                        }
                    }
                    String picProvence = uploadedImageIds.isEmpty() ? null : String.join(",", uploadedImageIds);
                    ApiService.createBookOrder(customer, customerId, details, picProvence, amount, price);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("新增存单成功");
                loadBookOrders();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                loading.setVisible(false);
                loadingLabel.setVisible(false);
                showError("新增存单失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        ScreenCaptureTool.showFloatingTrigger(addBookCaptureCb);
        dialog.setOnHidden(e -> ScreenCaptureTool.hideFloatingTrigger());
        dialog.showAndWait();
    }

    // ====================== 图片预览弹窗（支持翻页） ======================

    private void showImagePreviewDialog(String picProvence) {
        if (picProvence == null || picProvence.isBlank()) return;
        String[] ids = picProvence.split(",");
        if (ids.length == 0) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("图片预览");
        dialog.setHeaderText(null);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(500);
        imageView.setFitHeight(400);
        imageView.setPreserveRatio(true);

        Label pageLabel = new Label("1 / " + ids.length);
        pageLabel.setStyle("-fx-font-size: 14;");
        final int[] currentIndex = {0};

        // 直接通过公开 URL 加载图片
        Runnable loadImage = () -> {
            String url = ApiService.getImagePreviewUrl(ids[currentIndex[0]].trim());
            imageView.setImage(new Image(url, true));
            pageLabel.setText((currentIndex[0] + 1) + " / " + ids.length);
        };
        loadImage.run();

        Button prevBtn = new Button("上一张");
        prevBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 15;");
        prevBtn.setDisable(true);
        Button nextBtn = new Button("下一张");
        nextBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 15;");
        nextBtn.setDisable(ids.length <= 1);

        prevBtn.setOnAction(e -> {
            if (currentIndex[0] > 0) {
                currentIndex[0]--;
                loadImage.run();
                nextBtn.setDisable(false);
                prevBtn.setDisable(currentIndex[0] <= 0);
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentIndex[0] < ids.length - 1) {
                currentIndex[0]++;
                loadImage.run();
                prevBtn.setDisable(false);
                nextBtn.setDisable(currentIndex[0] >= ids.length - 1);
            }
        });

        HBox navBox = new HBox(15, prevBtn, pageLabel, nextBtn);
        navBox.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10, imageView, navBox);
        vb.setAlignment(Pos.CENTER);
        vb.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    // ====================== 多图上传公共组件 ======================

    /** 创建虚线框+号的添加图片按钮 */
    private Button createAddImageButton() {
        Button btn = new Button("+");
        btn.setPrefSize(80, 80);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #aaa; -fx-border-style: dashed; "
                + "-fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; "
                + "-fx-font-size: 24; -fx-text-fill: #aaa; -fx-cursor: hand;");
        return btn;
    }

    /** 图片选择弹出面板: 从文件选择 或 截图 */
    private void showImagePickerPopup(Dialog<?> dialog, List<File> imageFiles, List<String> uploadedIds,
                                      FlowPane imagePane, Button addImageBtn) {
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);

        Button fromFileBtn = new Button("从文件选择");
        fromFileBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; "
                + "-fx-padding: 8 16; -fx-background-radius: 5; -fx-font-size: 13;");
        Button fromCaptureBtn = new Button("截图");
        fromCaptureBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; "
                + "-fx-padding: 8 16; -fx-background-radius: 5; -fx-font-size: 13;");

        fromFileBtn.setOnAction(e -> {
            popup.hide();
            FileChooser fc = new FileChooser();
            fc.setTitle("选择图片");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) {
                addImageToPane(f, imageFiles, uploadedIds, imagePane, addImageBtn, dialog);
            }
        });
        fromCaptureBtn.setOnAction(e -> {
            popup.hide();
            ScreenCaptureTool.capture(dialog.getDialogPane().getScene().getWindow(), file -> {
                if (file != null) {
                    addImageToPane(file, imageFiles, uploadedIds, imagePane, addImageBtn, dialog);
                }
            });
        });

        HBox popupContent = new HBox(8, fromFileBtn, fromCaptureBtn);
        popupContent.setPadding(new Insets(8));
        popupContent.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        popup.getContent().add(popupContent);

        var bounds = addImageBtn.localToScreen(addImageBtn.getBoundsInLocal());
        popup.show(addImageBtn, bounds.getMinX(), bounds.getMaxY() + 4);
    }

    /** 添加一张图片到 FlowPane（缩略图 + 删除按钮） */
    private void addImageToPane(File file, List<File> imageFiles, List<String> uploadedIds,
                                FlowPane imagePane, Button addImageBtn, Dialog<?> dialog) {
        int index = imageFiles.size();
        imageFiles.add(file);
        uploadedIds.add(null); // 延迟上传

        ImageView thumb = new ImageView(new Image(file.toURI().toString(), 70, 70, true, true));
        thumb.setFitWidth(70);
        thumb.setFitHeight(70);

        Button removeBtn = new Button("×");
        removeBtn.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-cursor: hand; "
                + "-fx-padding: 0 4; -fx-font-size: 10; -fx-background-radius: 10;");

        StackPane imgContainer = new StackPane(thumb, removeBtn);
        imgContainer.setPrefSize(80, 80);
        StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);

        removeBtn.setOnAction(e -> {
            int idx = imagePane.getChildren().indexOf(imgContainer);
            if (idx >= 0) {
                imagePane.getChildren().remove(imgContainer);
                // 找到实际文件索引并移除
                int fileIdx = idx; // addImageBtn 始终在最后
                if (fileIdx < imageFiles.size()) {
                    imageFiles.remove(fileIdx);
                    uploadedIds.remove(fileIdx);
                }
            }
        });

        // 插入到"+"按钮之前
        int insertIdx = imagePane.getChildren().indexOf(addImageBtn);
        imagePane.getChildren().add(insertIdx, imgContainer);
    }

    // ====================== 请假记录 ======================

    private void setupLeaveTableColumns() {
        leaveTypeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getType() != null ? cd.getValue().getType() : ""));
        leaveApplyTimeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStartDate() != null ? cd.getValue().getStartDate() : ""));
        leaveEndTimeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getEndDate() != null ? cd.getValue().getEndDate() : ""));
        leaveStatusCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStatus() != null ? cd.getValue().getStatus() : ""));
    }

    private void loadLeaveRecords() {
        Task<ApiService.PageResult> task = new Task<>() {
            @Override
            protected ApiService.PageResult call() throws Exception {
                return ApiService.queryLeaveRecords(leaveCurrentPage, LEAVE_PAGE_SIZE);
            }
        };
        task.setOnSucceeded(e -> {
            ApiService.PageResult pr = task.getValue();
            List<LeaveRecord> records = pr.content.stream()
                    .map(LeaveRecord::fromMap).collect(Collectors.toList());
            leaveList.setAll(records);
            leaveTotalPages = Math.max(pr.totalPages, 1);
            leavePageLabel.setText("第 " + (leaveCurrentPage + 1) + " 页 / 共 " + leaveTotalPages + " 页");
            leavePrevBtn.setDisable(leaveCurrentPage <= 0);
            leaveNextBtn.setDisable(leaveCurrentPage >= leaveTotalPages - 1);
        });
        task.setOnFailed(e -> showError("加载请假记录失败: " + task.getException().getMessage()));
        runAsync(task);
    }

    @FXML
    private void handleRefreshLeave() { loadLeaveRecords(); }

    @FXML
    private void handleLeavePrev() {
        if (leaveCurrentPage > 0) { leaveCurrentPage--; loadLeaveRecords(); }
    }

    @FXML
    private void handleLeaveNext() {
        if (leaveCurrentPage < leaveTotalPages - 1) { leaveCurrentPage++; loadLeaveRecords(); }
    }

    @FXML
    private void handleAddLeave() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("新增请假");
        dialog.setHeaderText(null);

        ChoiceBox<String> typeBox = new ChoiceBox<>();
        typeBox.getItems().addAll("事假", "休假", "病假");
        typeBox.setValue("事假");
        typeBox.setPrefWidth(200);

        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        startDatePicker.setPrefWidth(200);
        startDatePicker.setPromptText("选择开始日期");

        DatePicker endDatePicker = new DatePicker(LocalDate.now());
        endDatePicker.setPrefWidth(200);
        endDatePicker.setPromptText("选择结束日期");

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("请输入请假事由");
        reasonField.setPrefRowCount(3);

        VBox vb = new VBox(10,
                new Label("假类型:"), typeBox,
                new Label("开始日期:"), startDatePicker,
                new Label("结束日期:"), endDatePicker,
                new Label("事由:"), reasonField);
        vb.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(380);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            String type = typeBox.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            String reason = reasonField.getText().trim();
            if (startDate == null) { showError("请选择开始日期"); return; }
            if (endDate == null) { showError("请选择结束日期"); return; }
            if (endDate.isBefore(startDate)) { showError("结束日期不能早于开始日期"); return; }
            if (reason.isEmpty()) { showError("请输入请假事由"); return; }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String startStr = startDate.format(fmt);
            String endStr = endDate.format(fmt);

            submitBtn.setDisable(true);
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ApiService.createLeaveRecord(type, reason, startStr, endStr);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("请假提交成功");
                loadLeaveRecords();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                showError("请假提交失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        dialog.showAndWait();
    }

    // ====================== 工资预支 ======================

    private void setupAdvanceTableColumns() {
        advanceNameCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getName() != null ? cd.getValue().getName() : ""));
        advanceAmountCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getAmount())));
        advanceStatusCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStatus() != null ? cd.getValue().getStatus() : ""));
    }

    private void loadAdvanceRecords() {
        Task<ApiService.PageResult> task = new Task<>() {
            @Override
            protected ApiService.PageResult call() throws Exception {
                return ApiService.querySalaryAdvances(advanceCurrentPage, ADVANCE_PAGE_SIZE);
            }
        };
        task.setOnSucceeded(e -> {
            ApiService.PageResult pr = task.getValue();
            List<SalaryAdvance> records = pr.content.stream()
                    .map(SalaryAdvance::fromMap).collect(Collectors.toList());
            advanceList.setAll(records);
            advanceTotalPages = Math.max(pr.totalPages, 1);
            advancePageLabel.setText("第 " + (advanceCurrentPage + 1) + " 页 / 共 " + advanceTotalPages + " 页");
            advancePrevBtn.setDisable(advanceCurrentPage <= 0);
            advanceNextBtn.setDisable(advanceCurrentPage >= advanceTotalPages - 1);
        });
        task.setOnFailed(e -> showError("加载预支记录失败: " + task.getException().getMessage()));
        runAsync(task);
    }

    @FXML
    private void handleRefreshAdvance() { loadAdvanceRecords(); }

    @FXML
    private void handleAdvancePrev() {
        if (advanceCurrentPage > 0) { advanceCurrentPage--; loadAdvanceRecords(); }
    }

    @FXML
    private void handleAdvanceNext() {
        if (advanceCurrentPage < advanceTotalPages - 1) { advanceCurrentPage++; loadAdvanceRecords(); }
    }

    @FXML
    private void handleAddAdvance() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("新增工资预支");
        dialog.setHeaderText(null);

        Spinner<Double> amountField = new Spinner<>(0, 999999, 0, 0.01);
        amountField.setEditable(true);
        amountField.setPrefWidth(200);

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("请输入预支事由");
        reasonField.setPrefRowCount(3);

        VBox vb = new VBox(10,
                new Label("预支金额:"), amountField,
                new Label("事由:"), reasonField);
        vb.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().setPrefWidth(380);

        ButtonType submitType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> null);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitType);
        submitBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            evt.consume();
            double amount;
            try {
                amount = Double.parseDouble(amountField.getEditor().getText().trim());
            } catch (NumberFormatException ex) {
                showError("金额格式错误"); return;
            }
            if (amount <= 0) { showError("金额必须大于0"); return; }
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) { showError("请输入预支事由"); return; }

            submitBtn.setDisable(true);
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ApiService.createSalaryAdvance(reason, amount);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                dialog.close();
                showInfo("预支申请提交成功");
                loadAdvanceRecords();
            });
            task.setOnFailed(e -> {
                submitBtn.setDisable(false);
                showError("预支申请失败: " + task.getException().getMessage());
            });
            runAsync(task);
        });

        dialog.showAndWait();
    }

    // ====================== 维护记录 ======================

    private void setupMaintTableColumns() {
        maintBossCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getBossName() != null ? cd.getValue().getBossName() : ""));
        maintWechatCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getWechatId() != null ? cd.getValue().getWechatId() : ""));
        maintTimeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getAddTime() != null ? cd.getValue().getAddTime() : ""));

        // 维护记录列: 显示"查看"链接
        maintLogCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("查看");
            {
                viewLink.setOnAction(e -> {
                    MaintenanceRecord rec = getTableView().getItems().get(getIndex());
                    showMaintDetail(rec);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                setGraphic(viewLink);
            }
        });
    }

    private void showMaintDetail(MaintenanceRecord rec) {
        selectedMaintRecord = rec;
        String name = rec.getBossName() != null ? rec.getBossName() : "";
        maintDetailTitle.setText("维护记录 — " + name);
        String log = rec.getMaintainLog();
        maintDetailContent.setText(log != null && !log.isBlank() ? log : "（暂无记录）");
        // 显示"+"按钮
        maintAddLogBtn.setVisible(true);
        maintAddLogBtn.setManaged(true);
        // 隐藏输入框
        maintInputField.setVisible(false);
        maintInputField.setManaged(false);
        maintInputActions.setVisible(false);
        maintInputActions.setManaged(false);
    }

    private void loadMaintRecords() {
        Task<ApiService.PageResult> task = new Task<>() {
            @Override
            protected ApiService.PageResult call() throws Exception {
                return ApiService.queryMaintenanceRecords(maintCurrentPage, MAINT_PAGE_SIZE);
            }
        };
        task.setOnSucceeded(e -> {
            ApiService.PageResult pr = task.getValue();
            List<MaintenanceRecord> records = pr.content.stream()
                    .map(MaintenanceRecord::fromMap).collect(Collectors.toList());
            maintList.setAll(records);
            maintTotalPages = Math.max(pr.totalPages, 1);
            maintPageLabel.setText("第 " + (maintCurrentPage + 1) + " 页 / 共 " + maintTotalPages + " 页");
            maintPrevBtn.setDisable(maintCurrentPage <= 0);
            maintNextBtn.setDisable(maintCurrentPage >= maintTotalPages - 1);
        });
        task.setOnFailed(e -> showError("加载维护记录失败: " + task.getException().getMessage()));
        runAsync(task);
    }

    @FXML
    private void handleRefreshMaint() { loadMaintRecords(); }

    @FXML
    private void handleMaintPrev() {
        if (maintCurrentPage > 0) { maintCurrentPage--; loadMaintRecords(); }
    }

    @FXML
    private void handleMaintNext() {
        if (maintCurrentPage < maintTotalPages - 1) { maintCurrentPage++; loadMaintRecords(); }
    }

    @FXML
    private void handleMaintAddLog() {
        // 显示输入框和确认/取消按钮，隐藏"+"
        maintInputField.setVisible(true);
        maintInputField.setManaged(true);
        maintInputField.setText("");
        maintInputField.requestFocus();
        maintInputActions.setVisible(true);
        maintInputActions.setManaged(true);
        maintAddLogBtn.setVisible(false);
        maintAddLogBtn.setManaged(false);

        // 限制50字
        maintInputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 50) {
                maintInputField.setText(newVal.substring(0, 50));
            }
        });
    }

    @FXML
    private void handleMaintConfirm() {
        if (selectedMaintRecord == null) { showError("请先选择一条维护记录"); return; }
        String content = maintInputField.getText().trim();
        if (content.isEmpty()) { showError("请输入维护内容"); return; }

        maintConfirmBtn.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ApiService.appendMaintenanceLog(selectedMaintRecord.getId(), content);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            maintConfirmBtn.setDisable(false);
            // 隐藏输入区域，恢复"+"
            maintInputField.setVisible(false);
            maintInputField.setManaged(false);
            maintInputActions.setVisible(false);
            maintInputActions.setManaged(false);
            maintAddLogBtn.setVisible(true);
            maintAddLogBtn.setManaged(true);
            // 刷新列表并更新详情
            loadMaintRecords();
            // 在详情内容后追加显示
            String existing = maintDetailContent.getText();
            String sep = (existing != null && !existing.isBlank() && !"（暂无记录）".equals(existing)) ? "\n" : "";
            maintDetailContent.setText(("（暂无记录）".equals(existing) ? "" : existing) + sep + content);
        });
        task.setOnFailed(e -> {
            maintConfirmBtn.setDisable(false);
            showError("保存失败: " + task.getException().getMessage());
        });
        runAsync(task);
    }

    @FXML
    private void handleMaintCancel() {
        maintInputField.setText("");
        maintInputField.setVisible(false);
        maintInputField.setManaged(false);
        maintInputActions.setVisible(false);
        maintInputActions.setManaged(false);
        maintAddLogBtn.setVisible(true);
        maintAddLogBtn.setManaged(true);
    }
}
