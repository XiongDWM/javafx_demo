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

import java.util.concurrent.atomic.AtomicBoolean;


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
    private final AtomicBoolean loginInProgress = new AtomicBoolean(false);
    private final AtomicBoolean loginResultHandled = new AtomicBoolean(false);

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
        if (!loginInProgress.compareAndSet(false, true)) {
            System.out.println("[Login] 登录进行中，忽略重复触发");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            loginInProgress.set(false);
            showError("请输入用户名");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            loginInProgress.set(false);
            showError("请输入密码");
            passwordField.requestFocus();
            return;
        }

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
            if (!loginResultHandled.compareAndSet(false, true)) {
                System.out.println("[Login] 已有成功登录结果被处理，忽略重复回调");
                return;
            }
            loginButton.setDisable(false);
            loginButton.setText("登录");
            loginInProgress.set(false);

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
            if (loginResultHandled.get()) {
                // 已经成功切页，后续失败回调不再影响 UI
                return;
            }
            loginButton.setDisable(false);
            loginButton.setText("登录");
            loginInProgress.set(false);
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
