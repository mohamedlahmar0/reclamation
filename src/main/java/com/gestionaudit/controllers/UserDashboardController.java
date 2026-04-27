package com.gestionaudit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import com.gestionaudit.utils.DialogUtils;
import java.io.IOException;

public class UserDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnReclamations;
    @FXML private Button btnChatbot;

    @FXML
    public void initialize() {
        // Load reclamations by default
        loadView("client_main.fxml", btnReclamations);
    }

    @FXML
    private void showReclamations() {
        loadView("client_main.fxml", btnReclamations);
    }

    @FXML
    private void showChatbot() {
        loadView("chatbot.fxml", btnChatbot);
    }
    
    @FXML
    private void handleLogout() {
        com.gestionaudit.MainFx.switchScene("/views/main_app.fxml");
    }

    private void loadView(String viewPath, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + viewPath));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // Update active state
            btnReclamations.getStyleClass().remove("active");
            btnChatbot.getStyleClass().remove("active");
            activeButton.getStyleClass().add("active");
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Error", "Could not load view: " + e.getMessage());
        }
    }
}
