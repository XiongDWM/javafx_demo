package javafx_demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx_demo.entity.Order;

/**
 * 工单表单控制器（已弃用 — 保留兼容性）
 *
 * 之前由 SceneManager.switchToOrderForm() 加载。
 * 现在统计功能已整合到 MainController 的 StackPane 视图中。
 */
public class OrderFormController {

    @FXML private TableView<Order> orderTable;
    @FXML private TextField searchField;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button searchBtn;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    @FXML private TableColumn<Order, String> idCol;
    @FXML private TableColumn<Order, String> priceCol;
    @FXML private TableColumn<Order, String> gameCol;
    @FXML private TableColumn<Order, String> quantityCol;
    @FXML private TableColumn<Order, String> statusCol;
    @FXML private TableColumn<Order, String> startCol;
    @FXML private TableColumn<Order, String> endCol;
    @FXML private TableColumn<Order, Void> actionCol;

    private ObservableList<Order> allOrders = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (idCol != null) idCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrderId()));
        if (statusCol != null) statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatusText()));
        if (orderTable != null) orderTable.setItems(allOrders);
        if (statusLabel != null) statusLabel.setText("此页面已不再使用，请返回主页");
    }

    @FXML private void handleAddOrder() {}
    @FXML private void handleEditOrder() {}
    @FXML private void handleDeleteOrder() {}
    @FXML private void handleSearch() {}
}
