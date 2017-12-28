package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.text.TextAlignment;

public class ServerWindowController {
    @FXML private ListView lstLog;

    public void log (String msg) {
        Platform.runLater(() ->{
            Label label= new Label(msg);
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.LEFT);
            lstLog.getItems().add(msg);
        });
    }
}
