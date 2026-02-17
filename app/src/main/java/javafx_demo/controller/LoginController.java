package javafx_demo.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx_demo.service.ApiService;
import javafx_demo.service.HttpService;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.SceneManager;
import javafx_demo.utils.SessionContext;


/**
 * Login Controller - 登录控制器（ECDH 握手 + /user/pal/login）
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label versionLabel;

    private ConfigManager configManager;

    @FXML
    public void initialize() {
        configManager = ConfigManager.getInstance();
        subtitleLabel.setText(configManager.getAppTitle());
        versionLabel.setText("Version " + configManager.getAppVersion());

        passwordField.setOnKeyPressed(this::handleKeyPressed);
        usernameField.setOnKeyPressed(this::handleKeyPressed);
        forgotPasswordLink.setOnAction(event -> showInfo("密码重置功能开发中..."));

        usernameField.focusedProperty().addListener((obs, o, n) -> { if (n) hideError(); });
        passwordField.focusedProperty().addListener((obs, o, n) -> { if (n) hideError(); });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) { showError("请输入用户名"); usernameField.requestFocus(); return; }
        if (password.isEmpty()) { showError("请输入密码"); passwordField.requestFocus(); return; }

        loginButton.setDisable(true);
        loginButton.setText("登录中...");
        hideError();

        Task<String> loginTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // 1. ECDH 密钥协商
                HttpService.handshake();
                // 2. 加密登录
                return ApiService.login(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            loginButton.setDisable(false);
            loginButton.setText("登录");

            String token = loginTask.getValue();
            SessionContext ctx = SessionContext.getInstance();
            ctx.setJwtToken(token);
            System.out.println("登录成功: userId=" + ctx.getUserId() + " username=" + ctx.getUsername());

            // 跳转主页
            MainController mc = SceneManager.getInstance()
                    .switchSceneWithController("/main.fxml", configManager.getAppTitle());
            if (mc != null) {
                mc.setUserInfo(ctx.getUsername());
            }
        });

        loginTask.setOnFailed(event -> {
            loginButton.setDisable(false);
            loginButton.setText("登录");
            Throwable ex = loginTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "未知错误";
            showError("登录失败: " + msg);
            ex.printStackTrace();
        });

        Thread t = new Thread(loginTask);
        t.setDaemon(true);
        t.start();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) handleLogin();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
