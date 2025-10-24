package application;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class AccueilController implements Initializable {

    @FXML
    private VBox alertesContainer;

    @FXML
    private Label lblTotalStagiaires;

    @FXML
    private Label lblTotalEcoles;

    @FXML
    private Label lblRetoursUrgents;

    private AlerteService alerteService;
    private StagiaireService stagiaireService;
    private EcoleService ecoleService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les services
        alerteService = new AlerteService();
        stagiaireService = new StagiaireService();
        ecoleService = new EcoleService();

        // Charger les données
        chargerStatistiques();
        chargerAlertes();
    }

    private void chargerStatistiques() {
        try {
            // Charger les statistiques
            int totalStagiaires = stagiaireService.compterStagiairesActifs();
            int totalEcoles = ecoleService.compterEcolesActives();
            int retoursUrgents = alerteService.compterRetoursUrgents(14); // Moins de 2 semaines

            // Mettre à jour l'interface
            Platform.runLater(() -> {
                lblTotalStagiaires.setText(String.valueOf(totalStagiaires));
                lblTotalEcoles.setText(String.valueOf(totalEcoles));
                lblRetoursUrgents.setText(String.valueOf(retoursUrgents));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerAlertes() {
        try {
            List<Alerte> alertes = alerteService.getAlertesActives();

            Platform.runLater(() -> {
                alertesContainer.getChildren().clear();

                for (Alerte alerte : alertes) {
                    HBox alerteBox = creerAlerteBox(alerte);
                    alertesContainer.getChildren().add(alerteBox);
                }

                // Si aucune alerte
                if (alertes.isEmpty()) {
                    Label aucuneAlerte = new Label("Aucune alerte pour le moment");
                    aucuneAlerte.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 20;");
                    alertesContainer.getChildren().add(aucuneAlerte);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox creerAlerteBox(Alerte alerte) {
        HBox box = new HBox(15);
        box.getStyleClass().add("alerte-item");

        // Déterminer le code couleur selon l'urgence
        long joursRestants = ChronoUnit.DAYS.between(
            java.time.LocalDate.now(), 
            alerte.getDateFinFormation()
        );

        String couleurClass;
        String urgenceText;

        if (joursRestants <= 14) {
            couleurClass = "alerte-critique"; // Rouge
            urgenceText = "URGENT - " + joursRestants + " jours";
        } else if (joursRestants <= 30) {
            couleurClass = "alerte-importante"; // Orange
            urgenceText = "Important - " + joursRestants + " jours";
        } else if (joursRestants <= 90) {
            couleurClass = "alerte-moyenne"; // Jaune
            urgenceText = "À surveiller - " + joursRestants + " jours";
        } else {
            couleurClass = "alerte-info"; // Vert
            urgenceText = "Informationnel - " + joursRestants + " jours";
        }

        box.getStyleClass().add(couleurClass);

        // Indicateur visuel
        VBox indicateur = new VBox();
        indicateur.setPrefWidth(8);
        indicateur.getStyleClass().add("alerte-indicateur");

        // Contenu de l'alerte
        VBox contenu = new VBox(5);
        contenu.setStyle("-fx-padding: 10;");

        Label titre = new Label(alerte.getNomStagiaire() + " - " + alerte.getTypeStagiaire());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label details = new Label(
            "École: " + alerte.getNomEcole() + " | " +
            "Pays: " + alerte.getPays() + " | " +
            "Fin: " + alerte.getDateFinFormation()
        );
        details.setStyle("-fx-font-size: 13px;");

        Label urgence = new Label(urgenceText);
        urgence.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        contenu.getChildren().addAll(titre, details, urgence);

        box.getChildren().addAll(indicateur, contenu);

        return box;
    }

    @FXML
    private void handleLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) alertesContainer.getScene().getWindow();
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleContinueWithoutLogin() {
        try {
            // Ouvrir le dashboard sans connexion (mode lecture seule)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainDashboard.fxml"));
            Parent root = loader.load();

            MainDashboardController controller = loader.getController();
            controller.setModeConsultation(true);

            Stage stage = (Stage) alertesContainer.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

            stage.setScene(scene);
            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }
}