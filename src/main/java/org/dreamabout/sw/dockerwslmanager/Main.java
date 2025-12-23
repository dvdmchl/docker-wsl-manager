package org.dreamabout.sw.dockerwslmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/main.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);

            primaryStage.setTitle("Docker WSL Manager");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        logger.info("Application stopping");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
