import connection.Server;
import controllers.ServerWindowController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Application.launch;

public class MainLauncher extends Application {
    public void start (Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ServerWindow.fxml"));

        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();

        ServerWindowController controller = loader.getController();

        Server server = new Server(9999);
        server.setController(controller);

        server.start();

    }

    public static void main (String[] args) {
        launch(args);
    }
}
