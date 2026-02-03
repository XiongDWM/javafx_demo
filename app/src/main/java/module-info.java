module javafx_demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // 如果使用第三方库（如 Guava），可能需要在这里添加相应的 requires
    // e.g. requires com.google.guava; // 请使用实际模块名或检查 jar 的 Automatic-Module-Name

    // JavaFX 的 FXML 通过反射访问 controller，所以需要 opens
    opens javafx_demo.controller to javafx.fxml;
    opens javafx_demo to javafx.fxml;

    // 对外暴露包（按需调整）
    exports javafx_demo;
    exports javafx_demo.entity;
    exports javafx_demo.service;
    exports javafx_demo.utils;
}
