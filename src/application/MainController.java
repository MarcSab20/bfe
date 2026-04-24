package application;

import application.utils.AlerteService;
import application.utils.Alerte;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label lblAlertCount;
    @FXML private Label lblUserInfo;
    @FXML private VBox  sidebarNav;

    private final AlerteService alerteService = new AlerteService();

    private String activeView = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lblUserInfo != null) lblUserInfo.setText("BFE – Gestion des Stages");
        chargerAlertes();
        // Ouvrir le tableau de bord par défaut
        handleShowTableauDeBord();
    }

    // ------------------------------------------------------------------ navigation

    @FXML
    public void handleShowTableauDeBord() {
        chargerVue("/application/statistiques/StatistiquesView.fxml", "dashboard");
    }

    @FXML
    public void handleShowEcoles() {
        chargerVue("/application/ecoles/EcolesView.fxml", "ecoles");
    }

    @FXML
    public void handleShowStagiaires() {
        chargerVue("/application/stagiaires/StagiairesView.fxml", "stagiaires");
    }

    @FXML
    public void handleShowStages() {
        chargerVue("/application/stages/StagesView.fxml", "stages");
    }

    @FXML
    public void handleShowCarte() {
        chargerVue("/application/carte/CarteView.fxml", "carte");
    }

    @FXML
    public void handleShowParametres() {
        chargerVue("/application/parametres/ParametresView.fxml", "parametres");
    }

    // ------------------------------------------------------------------ helpers

    private void chargerVue(String fxmlPath, String viewId) {
        if (viewId.equals(activeView)) return;
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showPlaceholder("Vue « " + viewId + " » non disponible.");
                return;
            }
            Node node = FXMLLoader.load(resource);
            if (contentArea != null) {
                contentArea.getChildren().setAll(node);
            }
            activeView = viewId;
            surlignerMenuActif(viewId);
        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder("Erreur lors du chargement : " + e.getMessage());
        }
    }

    private void showPlaceholder(String message) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #888;");
        if (contentArea != null) contentArea.getChildren().setAll(lbl);
    }

    private void surlignerMenuActif(String viewId) {
        if (sidebarNav == null) return;
        sidebarNav.getChildren().forEach(node -> {
            node.getStyleClass().remove("nav-item-active");
            if (viewId.equals(node.getUserData())) {
                node.getStyleClass().add("nav-item-active");
            }
        });
    }

    private void chargerAlertes() {
        new Thread(() -> {
            try {
                int nb = alerteService.compterRetoursUrgents(30);
                alerteService.genererAlertesAutomatiques(30);
                Platform.runLater(() -> {
                    if (lblAlertCount != null) {
                        lblAlertCount.setText(nb > 0 ? String.valueOf(nb) : "");
                        lblAlertCount.setVisible(nb > 0);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleShowAlertes() {
        new Thread(() -> {
            try {
                List<Alerte> alertes = alerteService.getAlertesUrgentes(30);
                Platform.runLater(() -> {
                    StringBuilder sb = new StringBuilder();
                    if (alertes.isEmpty()) {
                        sb.append("Aucune alerte urgente.");
                    } else {
                        for (Alerte a : alertes) sb.append("• ").append(a.getMessageAlerte()).append("\n");
                    }
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION, sb.toString());
                    alert.setTitle("Alertes de retour");
                    alert.setHeaderText("Stages se terminant dans les 30 prochains jours");
                    alert.showAndWait();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}
