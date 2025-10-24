package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialiser les connexions aux bases de données
            DatabaseManager.initialize();
            MongoDBManager.initialize();
            
            // Charger la page d'accueil
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Accueil.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1400, 800);
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            
            primaryStage.setTitle("Gestion du Personnel et des Stagiaires");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Fermer les connexions proprement
        DatabaseManager.close();
        MongoDBManager.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}