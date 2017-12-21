package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class ServerWindowController {
    @FXML
    ListView lstLog;

    public void log (String msg) {
        Platform.runLater(() -> lstLog.getItems().add(msg));
    }
}
