package javafx_demo.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.SceneManager;
import javafx_demo.service.OrderJsonLoader;


/**
 * Login Controller - Handles login page logic
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label versionLabel;

    private ConfigManager configManager;

    @FXML
    public void initialize() {
        configManager = ConfigManager.getInstance();
        
        // Set app title and version from config
        subtitleLabel.setText(configManager.getAppTitle());
        versionLabel.setText("Version " + configManager.getAppVersion());

        // Add Enter key listener for password field
        passwordField.setOnKeyPressed(this::handleKeyPressed);
        usernameField.setOnKeyPressed(this::handleKeyPressed);

        // Setup hyperlink actions
        forgotPasswordLink.setOnAction(event -> handleForgotPassword());

        // Add focus listener for error label
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) hideError();
        });
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) hideError();
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty()) {
            showError("请输入用户名");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("请输入密码");
            passwordField.requestFocus();
            return;
        }

        // Disable login button and show loading state
        loginButton.setDisable(true);
        loginButton.setText("登录中...");
        hideError();

        // Perform login in background thread
        Task<LoginResult> loginTask = new Task<LoginResult>() {
            @Override
            protected LoginResult call() throws Exception {
                return performLogin(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            LoginResult result = loginTask.getValue();
            loginButton.setDisable(false);
            loginButton.setText("登录");

            if (result.isSuccess()) {
                handleLoginSuccess(result);
            } else {
                showError(result.getMessage());
            }
        });

        loginTask.setOnFailed(event -> {
            loginButton.setDisable(false);
            loginButton.setText("登录");
            Throwable exception = loginTask.getException();
            showError("登录失败: " + exception.getMessage());
        });

        // Start the task in a background thread
        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    private LoginResult performLogin(String username, String password) {
        // 🔧 开发模式：直接模拟登录成功（无需后端服务器）
        // 生产环境请注释掉下面这段代码
        System.out.println("🔧 开发模式：模拟登录成功");
        String mockResponse = String.format(
            "{\"code\":200,\"message\":\"登录成功\",\"data\":{\"username\":\"%s\",\"token\":\"mock_token_12345\",\"userId\":\"1001\"}}",
            username
        );
        return new LoginResult(true, "登录成功", mockResponse);
        // 🔧 开发模式代码结束
        
        /* 生产环境代码 - 取消注释以启用真实API调用
        try {
            String loginUrl = ApiConfig.getLoginUrl();
            URL url = new URL(loginUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set connection properties
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(configManager.getConnectionTimeout());
            connection.setReadTimeout(configManager.getReadTimeout());
            connection.setDoOutput(true);

            // Create JSON payload
            String jsonPayload = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password
            );

            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                return new LoginResult(true, "登录成功", response.toString());
            } else {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                return new LoginResult(false, "登录失败: " + responseCode, response.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new LoginResult(false, "连接服务器失败: " + e.getMessage(), null);
        }
        */
    }

    private void handleLoginSuccess(LoginResult result) {
        System.out.println("Login successful!");
        System.out.println("Response: " + result.getData());
        
        // 登录成功 - 加载工单数据到缓存
        System.out.println("\n✅ 登录成功，开始加载工单数据...");
        OrderJsonLoader.loadOrders();
        System.out.println("✅ 工单数据已加载到缓存\n");
        
        // 获取用户名
        String username = usernameField.getText().trim();
        
        // TODO: 保存token到本地缓存
        // 例如: TokenManager.getInstance().saveToken(token);
        
        // 跳转到主页面
        MainController mainController = SceneManager.getInstance()
            .switchSceneWithController("/main.fxml", configManager.getAppTitle());
        
        // 设置用户信息
        if (mainController != null) {
            mainController.setUserInfo(username);
        }
    }

    private void handleForgotPassword() {
        showInfo("密码重置功能开发中...");
        // TODO: Implement forgot password logic
    }

    private void handleRegister() {
        showInfo("注册功能开发中...");
        // TODO: Navigate to registration page
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
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

    /**
     * Inner class to hold login result
     */
    private static class LoginResult {
        private final boolean success;
        private final String message;
        private final String data;

        public LoginResult(boolean success, String message, String data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getData() {
            return data;
        }
    }
}
