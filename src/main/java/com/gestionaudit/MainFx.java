package com.gestionaudit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainFx extends Application {

    private static Stage primaryStage;

    /**
     * Applies {@code modern.css} from classpath ({@code /styles/modern.css}) to the scene.
     * Prefer this over relying only on FXML {@code stylesheets} so styling works in every run configuration.
     */
    public static void attachModernStylesheet(Scene scene) {
        URL url = MainFx.class.getResource("/styles/modern.css");
        if (url == null) {
            System.err.println("WARNING: /styles/modern.css not found on classpath — check resources folder.");
            return;
        }
        String href = url.toExternalForm();
        if (!scene.getStylesheets().contains(href)) {
            scene.getStylesheets().add(href);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Gestion Audit - Système de Réclamations");
        switchScene("/views/main_app.fxml");
        primaryStage.show();
    }

    /** Primary stage; null before {@link Application#start} completes. */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            attachModernStylesheet(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error switching to scene: " + fxmlPath);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
