module javafx_demo {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires org.json;

    // JavaFX 的 FXML 通过反射访问 controller，所以需要 opens
    opens javafx_demo.controller to javafx.fxml;
    opens javafx_demo to javafx.fxml;
    opens javafx_demo.entity to javafx.base;

    // 对外暴露包（按需调整）
    exports javafx_demo;
    exports javafx_demo.entity;
    exports javafx_demo.service;
    exports javafx_demo.utils;
}
