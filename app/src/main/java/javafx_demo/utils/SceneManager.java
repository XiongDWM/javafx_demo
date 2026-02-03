package javafx_demo.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Scene Manager - 统一管理页面跳转
 */
public class SceneManager {
    private static Stage primaryStage;
    private static SceneManager instance;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            synchronized (SceneManager.class) {
                if (instance == null) {
                    instance = new SceneManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化主舞台
     */
    public void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * 获取主舞台
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * 切换场景
     * @param fxmlPath FXML文件路径（相对于resources目录）
     * @param title 窗口标题
     */
    public void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("无法加载页面: " + fxmlPath);
        }
    }

    /**
     * 切换场景并返回Controller
     * @param fxmlPath FXML文件路径
     * @param title 窗口标题
     * @return Controller实例
     */
    public <T> T switchSceneWithController(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.centerOnScreen();
            
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("无法加载页面: " + fxmlPath);
            return null;
        }
    }

    /**
     * 加载新窗口
     * @param fxmlPath FXML文件路径
     * @param title 窗口标题
     * @return 新的Stage
     */
    public Stage openNewWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage newStage = new Stage();
            Scene scene = new Scene(root);
            newStage.setScene(scene);
            newStage.setTitle(title);
            newStage.show();
            
            return newStage;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("无法打开新窗口: " + fxmlPath);
            return null;
        }
    }

    /**
     * 切换到登录页面
     */
    public void switchToLogin() {
        ConfigManager config = ConfigManager.getInstance();
        switchScene("/login.fxml", config.getAppTitle() + " - 登录");
        primaryStage.setResizable(false);
    }

    /**
     * 切换到主页面
     */
    public void switchToMain() {
        ConfigManager config = ConfigManager.getInstance();
        switchScene("/main.fxml", config.getAppTitle());
        primaryStage.setResizable(true);
    }

    /**
     * 切换到个人信息页面
     */
    public void switchToUserInfo() {
        ConfigManager config = ConfigManager.getInstance();
        switchScene("/userinfo.fxml", config.getAppTitle() + " - 个人信息");
    }

    /**
     * 切换到工单管理页面
     */
    public void switchToOrderForm() {
        ConfigManager config = ConfigManager.getInstance();
        switchScene("/orderform.fxml", config.getAppTitle() + " - 工单管理");
    }
}
