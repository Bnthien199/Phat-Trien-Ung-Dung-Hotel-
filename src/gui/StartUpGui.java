package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.SwingLookAndFeel;

public class StartUpGui extends Application {

    @Override
    public void start(Stage primaryStage) {
        SwingLookAndFeel.configure();
        Platform.setImplicitExit(false);

        primaryStage.setTitle("Đăng nhập — Hệ Thống Quản Lý Khách Sạn");
        LoginFrame login = new LoginFrame();
        Scene scene = login.createScene(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(550);
        primaryStage.show();
    }

    public static void main(String[] args) {
        SwingLookAndFeel.configure();
        launch(args);
    }
}
