package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private ComboBox<String> cmbNiveauAcces;

    @FXML
    private Label lblError;

    private AuthentificationService authService;
    private AuditService auditService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthentificationService();
        auditService = new AuditService();
        
        // Sélectionner le premier niveau par défaut
        cmbNiveauAcces.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleLogin() {
        // Réinitialiser le message d'erreur
        lblError.setVisible(false);
        
        // Validation des champs
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String niveauAcces = cmbNiveauAcces.getValue();

        if (username.isEmpty()) {
            afficherErreur("Veuillez entrer votre nom d'utilisateur");
            return;
        }

        if (password.isEmpty()) {
            afficherErreur("Veuillez entrer votre mot de passe");
            return;
        }

        if (niveauAcces == null) {
            afficherErreur("Veuillez sélectionner votre niveau d'accès");
            return;
        }

        try {
            // Tentative d'authentification
            Utilisateur utilisateur = authService.authentifier(username, password, niveauAcces);

            if (utilisateur != null) {
                // Enregistrer l'événement de connexion
                auditService.enregistrerEvenement(
                    utilisateur.getId(),
                    "CONNEXION",
                    "Connexion réussie",
                    "Niveau: " + utilisateur.getNiveauAcces(),
                    LocalDateTime.now()
                );

                // Ouvrir le dashboard principal
                ouvrirDashboard(utilisateur);
            } else {
                afficherErreur("Nom d'utilisateur ou mot de passe incorrect");
                
                // Enregistrer la tentative échouée
                auditService.enregistrerEvenement(
                    null,
                    "ECHEC_CONNEXION",
                    "Tentative de connexion échouée",
                    "Username: " + username,
                    LocalDateTime.now()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur lors de la connexion: " + e.getMessage());
        }
    }

    private void ouvrirDashboard(Utilisateur utilisateur) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainDashboard.fxml"));
            Parent root = loader.load();

            // Passer les informations utilisateur au dashboard
            MainDashboardController controller = loader.getController();
            controller.setUtilisateur(utilisateur);
            controller.setModeConsultation(false);

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur lors de l'ouverture du dashboard");
        }
    }

    private void afficherErreur(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    @FXML
    private void handleCancel() {
        try {
            // Retour à la page d'accueil
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Accueil.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot de passe oublié");
        alert.setHeaderText("Réinitialisation du mot de passe");
        alert.setContentText("Veuillez contacter l'administrateur système pour réinitialiser votre mot de passe.");
        alert.showAndWait();
    }
}