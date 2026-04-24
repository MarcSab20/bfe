import application.utils.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Point d'entrée de l'application BFE – Gestion des Stages.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlUrl = getClass().getResource("/application/MainView.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("MainView.fxml introuvable dans le classpath.");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        URL cssUrl = getClass().getResource("/application/styles/styles.css");
        Scene scene = new Scene(root, 1280, 800);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("BFE – Gestion des Stages et des Écoles");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
