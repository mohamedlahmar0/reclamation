package com.gestionaudit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import com.gestionaudit.utils.DialogUtils;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnReclamations;
    @FXML private Button btnStats;
    @FXML private Button btnMessages;

    @FXML
    public void initialize() {
        // Load reclamations by default
        loadView("admin_main.fxml", btnReclamations);
    }

    @FXML
    private void showReclamations() {
        loadView("admin_main.fxml", btnReclamations);
    }

    @FXML
    private void showStats() {
        loadView("stats.fxml", btnStats);
    }

    @FXML
    private void showMessages() {
        loadView("admin_messages.fxml", btnMessages);
    }
    
    @FXML
    private void handleLogout() {
        // Simple logout back to main app
        com.gestionaudit.MainFx.switchScene("/views/main_app.fxml");
    }

    private void loadView(String viewPath, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + viewPath));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // Update active state of buttons
            btnReclamations.getStyleClass().remove("active");
            btnStats.getStyleClass().remove("active");
            btnMessages.getStyleClass().remove("active");
            activeButton.getStyleClass().add("active");
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Error", "Could not load view: " + e.getMessage());
        }
    }
}
