package com.gestionaudit.controllers;

import com.gestionaudit.MainFx;
import javafx.fxml.FXML;

public class MainAppController {

    @FXML
    private void goToClient() {
        MainFx.switchScene("/views/user_dashboard.fxml");
    }

    @FXML
    private void goToAdmin() {
        MainFx.switchScene("/views/admin_dashboard.fxml");
    }
}
