package javafx_demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx_demo.utils.ConfigManager;
import javafx_demo.utils.SceneManager;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 初始化SceneManager
        SceneManager.getInstance().setPrimaryStage(primaryStage);
        
        ConfigManager config = ConfigManager.getInstance();
        
        // 加载登录页面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        primaryStage.setTitle(config.getAppTitle() + " - 登录");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
