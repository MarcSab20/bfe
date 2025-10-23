package application;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import netscape.javascript.JSObject;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainDashboardController implements Initializable {

    // Composants de l'interface
    @FXML private Label lblUtilisateur;
    @FXML private Label lblNiveauAcces;
    @FXML private HBox alertBarContainer;
    @FXML private Label lblMapInfo;
    
    // Sidebar buttons
    @FXML private Button btnEmplacement;
    @FXML private Button btnStatistiques;
    @FXML private Button btnEcoles;
    @FXML private Button btnStages;
    @FXML private Button btnStagiaires;
    @FXML private Button btnTravauxRecurrents;
    
    // Vues
    @FXML private HBox viewEmplacement;
    @FXML private VBox viewStatistiques;
    @FXML private VBox viewEcoles;
    @FXML private VBox viewStages;
    @FXML private VBox viewStagiaires;
    
    // Carte
    @FXML private WebView mapWebView;
    @FXML private ComboBox<String> cmbTypeAffichage;
    @FXML private ComboBox<String> cmbSpecialite;
    @FXML private ComboBox<String> cmbLangue;
    
    // Panneau de détails
    @FXML private VBox detailsPanel;
    @FXML private VBox detailsContent;
    
    // Services
    private AlerteService alerteService;
    private EcoleService ecoleService;
    private StagiaireService stagiaireService;
    private StageService stageService;
    private AuditService auditService;
    
    // État
    private Utilisateur utilisateurConnecte;
    private boolean modeConsultation = false;
    private WebEngine webEngine;
    private Gson gson = new Gson();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les services
        alerteService = new AlerteService();
        ecoleService = new EcoleService();
        stagiaireService = new StagiaireService();
        stageService = new StageService();
        auditService = new AuditService();
        
        // Initialiser la carte
        initialiserCarte();
        
        // Charger les filtres
        chargerFiltres();
        
        // Charger les alertes dans la barre
        chargerAlertesBar();
        
        // Sélectionner "Tous" par défaut
        cmbTypeAffichage.getSelectionModel().selectFirst();
    }
    
    private void initialiserCarte() {
        webEngine = mapWebView.getEngine();
        
        // Désactiver le menu contextuel
        mapWebView.setContextMenuEnabled(false);
        
        // Charger la page HTML de la carte
        try {
            URL mapUrl = getClass().getResource("/map/leaflet.html");
            if (mapUrl != null) {
                webEngine.load(mapUrl.toExternalForm());
            } else {
                System.err.println("Fichier leaflet.html introuvable!");
                return;
            }
            
            // Configurer le bridge Java-JavaScript une fois la page chargée
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    // Exposer l'objet Java au JavaScript
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaApp", new JavaScriptBridge());
                    
                    // Charger les marqueurs initiaux
                    chargerMarqueursCarte();
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void chargerMarqueursCarte() {
        try {
            // Récupérer les données selon le filtre
            String typeAffichage = cmbTypeAffichage.getValue();
            String specialite = cmbSpecialite.getValue();
            String langue = cmbLangue.getValue();
            
            if (typeAffichage == null || typeAffichage.equals("Tous")) {
                chargerToutesLesMarqueurs();
            } else if (typeAffichage.contains("Écoles")) {
                chargerMarqueursEcoles(typeAffichage, specialite);
            } else if (typeAffichage.equals("Stagiaires")) {
                chargerMarqueursStagiaires(specialite, langue);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void chargerToutesLesMarqueurs() {
        try {
            // Récupérer toutes les écoles
            List<Ecole> ecoles = ecoleService.getToutesEcoles();
            
            for (Ecole ecole : ecoles) {
                if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                    String couleur = determinerCouleurEcole(ecole);
                    String infobulle = creerInfobulleEcole(ecole);
                    
                    ajouterMarqueur(
                        ecole.getLatitude(),
                        ecole.getLongitude(),
                        ecole.getNom(),
                        infobulle,
                        couleur,
                        "ecole",
                        ecole.getId()
                    );
                }
            }
            
            // Récupérer tous les stagiaires actifs
            List<Stagiaire> stagiaires = stagiaireService.getStagiairesActifs();
            
            for (Stagiaire stagiaire : stagiaires) {
                StageFormation stage = stageService.getStageActifByStagiaire(stagiaire.getId());
                if (stage != null && stage.getEcole() != null) {
                    Ecole ecole = stage.getEcole();
                    if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                        String infobulle = creerInfobulleStagiaire(stagiaire, stage);
                        
                        ajouterMarqueur(
                            ecole.getLatitude(),
                            ecole.getLongitude(),
                            stagiaire.getNom() + " " + stagiaire.getPrenom(),
                            infobulle,
                            "blue",
                            "stagiaire",
                            stagiaire.getId()
                        );
                    }
                }
            }
            
            Platform.runLater(() -> {
                lblMapInfo.setText("Affichage: " + ecoles.size() + " écoles, " + 
                                 stagiaires.size() + " stagiaires");
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void chargerMarqueursEcoles(String type, String specialite) {
        try {
            List<Ecole> ecoles;
            
            if (type.equals("Écoles Professionnelles")) {
                ecoles = ecoleService.getEcolesParType("Professionnelle");
            } else if (type.equals("Écoles Académiques")) {
                ecoles = ecoleService.getEcolesParType("Académique");
            } else if (type.equals("Écoles Partenaires")) {
                ecoles = ecoleService.getEcolesPartenaires();
            } else {
                ecoles = ecoleService.getToutesEcoles();
            }
            
            // Filtrer par spécialité si nécessaire
            if (specialite != null && !specialite.isEmpty()) {
                ecoles = ecoleService.getToutesEcoles().stream()
                    .filter(e -> e.getSpecialites() != null &&
                            e.getSpecialites().contains(specialite))
                    .collect(Collectors.toList());
            } else {
                ecoles = ecoleService.getToutesEcoles();
            }
            
            for (Ecole ecole : ecoles) {
                if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                    String couleur = determinerCouleurEcole(ecole);
                    String infobulle = creerInfobulleEcole(ecole);
                    
                    ajouterMarqueur(
                        ecole.getLatitude(),
                        ecole.getLongitude(),
                        ecole.getNom(),
                        infobulle,
                        couleur,
                        "ecole",
                        ecole.getId()
                    );
                }
            }
            
            Platform.runLater(() -> {
                lblMapInfo.setText("Affichage: " + ecoles.size() + " écoles");
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void chargerMarqueursStagiaires(String specialite, String langue) {
        try {
            List<Stagiaire> stagiaires = stagiaireService.getStagiairesActifs();
            
            // Filtrer par spécialité et langue
            if (specialite != null && !specialite.isEmpty()) {
                stagiaires = stagiaires.stream()
                    .filter(s -> s.getSpecialite() != null && 
                               s.getSpecialite().equals(specialite))
                    .toList();
            }
            
            if (langue != null && !langue.isEmpty()) {
                stagiaires = stagiaires.stream()
                    .filter(s -> s.getLangue() != null && 
                               s.getLangue().equals(langue))
                    .toList();
            }
            
            for (Stagiaire stagiaire : stagiaires) {
                Stage stage = stageService.getStageActifByStagiaire(stagiaire.getId());
                if (stage != null && stage.getEcole() != null) {
                    Ecole ecole = stage.getEcole();
                    if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                        String infobulle = creerInfobulleStagiaire(stagiaire, stage);
                        
                        ajouterMarqueur(
                            ecole.getLatitude(),
                            ecole.getLongitude(),
                            stagiaire.getNom() + " " + stagiaire.getPrenom(),
                            infobulle,
                            "blue",
                            "stagiaire",
                            stagiaire.getId()
                        );
                    }
                }
            }
            
            Platform.runLater(() -> {
                lblMapInfo.setText("Affichage: " + stagiaires.size() + " stagiaires");
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void ajouterMarqueur(Double lat, Double lng, String titre, String infobulle, 
                                  String couleur, String type, Long id) {
        Platform.runLater(() -> {
            String script = String.format(
                "addMarker(%f, %f, '%s', '%s', '%s', '%s', %d);",
                lat, lng, 
                echapperPourJS(titre), 
                echapperPourJS(infobulle),
                couleur,
                type,
                id
            );
            webEngine.executeScript(script);
        });
    }
    
    private String echapperPourJS(String texte) {
        if (texte == null) return "";
        return texte.replace("'", "\\'")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }
    
    private String determinerCouleurEcole(Ecole ecole) {
        if (ecole.getType() != null) {
            switch (ecole.getType()) {
                case "Professionnelle": return "green";
                case "Académique": return "orange";
                case "Partenaire": return "purple";
                default: return "red";
            }
        }
        return "red";
    }
    
    private String creerInfobulleEcole(Ecole ecole) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(ecole.getNom()).append("</b><br>");
        sb.append("Type: ").append(ecole.getType() != null ? ecole.getType() : "N/A").append("<br>");
        sb.append("Pays: ").append(ecole.getPays() != null ? ecole.getPays() : "N/A").append("<br>");
        sb.append("Ville: ").append(ecole.getVille() != null ? ecole.getVille() : "N/A");
        return sb.toString();
    }
    
    private String creerInfobulleStagiaire(Stagiaire stagiaire, StageFormation stage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(stagiaire.getNom()).append(" ").append(stagiaire.getPrenom()).append("</b><br>");
        sb.append("Spécialité: ").append(stagiaire.getSpecialite() != null ? stagiaire.getSpecialite() : "N/A").append("<br>");
        sb.append("École: ").append(stage.getEcole().getNom()).append("<br>");
        sb.append("Fin: ").append(stage.getDateFin() != null ? 
            stage.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
        return sb.toString();
    }
    
    private void chargerFiltres() {
        try {
            // Charger les spécialités
            List<String> specialites = stagiaireService.getToutesSpecialites();
            cmbSpecialite.getItems().clear();
            cmbSpecialite.getItems().add(null); // Option vide
            cmbSpecialite.getItems().addAll(specialites);
            
            // Charger les langues
            List<String> langues = stagiaireService.getToutesLangues();
            cmbLangue.getItems().clear();
            cmbLangue.getItems().add(null); // Option vide
            cmbLangue.getItems().addAll(langues);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void chargerAlertesBar() {
        try {
            List<Alerte> alertes = alerteService.getAlertesUrgentes(30); // 30 jours
            
            Platform.runLater(() -> {
                alertBarContainer.getChildren().clear();
                
                for (Alerte alerte : alertes) {
                    Label lblAlerte = new Label(
                        alerte.getNomStagiaire() + " - Retour: " + 
                        alerte.getDateFinFormation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    );
                    
                    lblAlerte.setStyle(
                        "-fx-background-color: #ff6b6b; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 5 10; " +
                        "-fx-background-radius: 3;"
                    );
                    
                    alertBarContainer.getChildren().add(lblAlerte);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Bridge Java-JavaScript pour la communication bidirectionnelle
    public class JavaScriptBridge {
        public void onMarkerClick(String type, long id) {
            Platform.runLater(() -> afficherDetails(type, id));
        }
    }
    
    private void afficherDetails(String type, long id) {
        detailsContent.getChildren().clear();
        
        try {
            if (type.equals("ecole")) {
                Ecole ecole = ecoleService.getEcoleParId(id);
                if (ecole != null) {
                    afficherDetailsEcole(ecole);
                }
            } else if (type.equals("stagiaire")) {
                Stagiaire stagiaire = stagiaireService.getStagiaireParId(id);
                if (stagiaire != null) {
                    afficherDetailsStagiaire(stagiaire);
                }
            }
            
            detailsPanel.setVisible(true);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void afficherDetailsEcole(Ecole ecole) {
        Label titre = new Label(ecole.getNom());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        VBox infos = new VBox(8);
        infos.getChildren().addAll(
            creerLigneDetail("Type", ecole.getType()),
            creerLigneDetail("Pays", ecole.getPays()),
            creerLigneDetail("Ville", ecole.getVille()),
            creerLigneDetail("Adresse", ecole.getAdresse()),
            creerLigneDetail("Contact", ecole.getContact()),
            creerLigneDetail("Email", ecole.getEmail())
        );
        
        // Compter les stagiaires dans cette école
        int nbStagiaires = stagiaireService.compterStagiairesParEcole(ecole.getId());
        Label lblStagiaires = new Label("Stagiaires actuels: " + nbStagiaires);
        lblStagiaires.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        detailsContent.getChildren().addAll(titre, new Separator(), infos, new Separator(), lblStagiaires);
    }
    
    private void afficherDetailsStagiaire(Stagiaire stagiaire) {
        Label titre = new Label(stagiaire.getNom() + " " + stagiaire.getPrenom());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        VBox infos = new VBox(8);
        infos.getChildren().addAll(
            creerLigneDetail("Matricule", stagiaire.getMatricule()),
            creerLigneDetail("Spécialité", stagiaire.getSpecialite()),
            creerLigneDetail("Langue", stagiaire.getLangue()),
            creerLigneDetail("Email", stagiaire.getEmail()),
            creerLigneDetail("Téléphone", stagiaire.getTelephone())
        );
        
        // Afficher le stage actif
        StageFormation stage = stageService.getStageActifByStagiaire(stagiaire.getId());
        if (stage != null) {
            Label lblStage = new Label("Stage Actuel");
            lblStage.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            VBox infosStage = new VBox(8);
            infosStage.getChildren().addAll(
                creerLigneDetail("École", stage.getEcole().getNom()),
                creerLigneDetail("Type", stage.getType()),
                creerLigneDetail("Début", stage.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                creerLigneDetail("Fin", stage.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            );
            
            detailsContent.getChildren().addAll(
                titre, new Separator(), infos, 
                new Separator(), lblStage, infosStage
            );
        } else {
            detailsContent.getChildren().addAll(titre, new Separator(), infos);
        }
    }
    
    private HBox creerLigneDetail(String label, String valeur) {
        HBox ligne = new HBox(10);
        
        Label lblLabel = new Label(label + ":");
        lblLabel.setStyle("-fx-font-weight: bold;");
        lblLabel.setPrefWidth(100);
        
        Label lblValeur = new Label(valeur != null ? valeur : "N/A");
        lblValeur.setWrapText(true);
        
        ligne.getChildren().addAll(lblLabel, lblValeur);
        return ligne;
    }
    
    // Gestionnaires d'événements de la sidebar
    @FXML
    private void handleEmplacement() {
        changerVue(viewEmplacement, btnEmplacement);
    }
    
    @FXML
    private void handleStatistiques() {
        changerVue(viewStatistiques, btnStatistiques);
    }
    
    @FXML
    private void handleGestionEcoles() {
        changerVue(viewEcoles, btnEcoles);
    }
    
    @FXML
    private void handleGestionStages() {
        changerVue(viewStages, btnStages);
    }
    
    @FXML
    private void handleGestionStagiaires() {
        changerVue(viewStagiaires, btnStagiaires);
    }
    
    @FXML
    private void handleTravauxRecurrents() {
        // À implémenter
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Travaux Récurrents");
        alert.setHeaderText("Module en développement");
        alert.setContentText("Cette fonctionnalité sera disponible prochainement.");
        alert.showAndWait();
    }
    
    @FXML
    private void handleParametres() {
        // À implémenter
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paramètres");
        alert.setHeaderText("Paramètres de l'application");
        alert.setContentText("Cette fonctionnalité sera disponible prochainement.");
        alert.showAndWait();
    }
    
    private void changerVue(VBox nouvelleVue, Button bouton) {
        // Masquer toutes les vues
        viewEmplacement.setVisible(false);
        viewEmplacement.setManaged(false);
        viewStatistiques.setVisible(false);
        viewStatistiques.setManaged(false);
        viewEcoles.setVisible(false);
        viewEcoles.setManaged(false);
        viewStages.setVisible(false);
        viewStages.setManaged(false);
        viewStagiaires.setVisible(false);
        viewStagiaires.setManaged(false);
        
        // Désactiver tous les boutons
        btnEmplacement.getStyleClass().remove("sidebar-btn-active");
        btnStatistiques.getStyleClass().remove("sidebar-btn-active");
        btnEcoles.getStyleClass().remove("sidebar-btn-active");
        btnStages.getStyleClass().remove("sidebar-btn-active");
        btnStagiaires.getStyleClass().remove("sidebar-btn-active");
        
        // Afficher la nouvelle vue
        nouvelleVue.setVisible(true);
        nouvelleVue.setManaged(true);
        
        // Activer le bouton
        bouton.getStyleClass().add("sidebar-btn-active");
    }
    
    @FXML
    private void handleRechercherCarte() {
        // Réinitialiser la carte
        webEngine.executeScript("clearMarkers();");
        
        // Recharger avec les nouveaux filtres
        chargerMarqueursCarte();
    }
    
    @FXML
    private void handleResetCarte() {
        cmbTypeAffichage.getSelectionModel().selectFirst();
        cmbSpecialite.getSelectionModel().clearSelection();
        cmbLangue.getSelectionModel().clearSelection();
        
        handleRechercherCarte();
    }
    
    @FXML
    private void handleFermerDetails() {
        detailsPanel.setVisible(false);
    }
    
    @FXML
    private void handleDeconnexion() {
        if (utilisateurConnecte != null) {
            auditService.enregistrerEvenement(
                utilisateurConnecte.getId(),
                "DECONNEXION",
                "Déconnexion",
                "Fin de session",
                java.time.LocalDateTime.now()
            );
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Accueil.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) lblUtilisateur.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Setters pour configurer le controller
    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
        lblUtilisateur.setText("Utilisateur: " + utilisateur.getNomUtilisateur());
        lblNiveauAcces.setText(utilisateur.getNiveauAcces());
    }
    
    public void setModeConsultation(boolean mode) {
        this.modeConsultation = mode;
        if (mode) {
            lblUtilisateur.setText("Mode Consultation");
            lblNiveauAcces.setText("Lecture seule");
        }
    }
}