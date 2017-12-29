package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.text.TextAlignment;
import org.apache.commons.lang3.text.WordUtils;

public class ServerWindowController {
    @FXML private ListView lstLog;

    public void log (String msg) {
        Platform.runLater(() ->{
            lstLog.getItems().add(WordUtils.wrap(msg, 45));
            lstLog.scrollTo(lstLog.getItems().size() - 1);
        });
    }
}
