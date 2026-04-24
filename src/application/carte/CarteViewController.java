package application.carte;

import application.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller pour la vue Carte Interactive
 */
public class CarteViewController implements Initializable {
    
    @FXML private StackPane mapContainer;
    @FXML private ComboBox<String> cmbTypeAffichage;
    @FXML private ComboBox<String> cmbPays;
    @FXML private ComboBox<String> cmbSpecialite;
    @FXML private ComboBox<String> cmbLangue;
    @FXML private Label lblTotalEcoles;
    @FXML private Label lblTotalStagiaires;
    @FXML private Label lblConnexion;
    @FXML private Slider sliderZoom;
    @FXML private Label lblZoomLevel;
    @FXML private ProgressIndicator progressLoading;
    @FXML private Label lblMapInfo;
    @FXML private VBox detailsPanel;
    @FXML private VBox detailsContent;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    
    // Services
    private EcoleService ecoleService;
    private StagiaireService stagiaireService;
    private StageService stageService;
    
    // Carte offline
    private OfflineMapView carte;
    
    // Données courantes
    private List<Ecole> ecolesCourantes = new ArrayList<>();
    private List<Stagiaire> stagiairesCourants = new ArrayList<>();
    private Object elementSelectionne; // Ecole ou Stagiaire
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les services
        ecoleService = new EcoleService();
        stagiaireService = new StagiaireService();
        stageService = new StageService();
        
        // Créer et ajouter la carte offline
        carte = new OfflineMapView();
        mapContainer.getChildren().add(carte);
        
        // Configurer le listener de clic sur marqueur
        carte.setMarkerClickListener(this::handleMarkerClick);
        
        // Charger les filtres
        chargerFiltres();
        
        // Sélectionner le premier filtre par défaut
        cmbTypeAffichage.getSelectionModel().selectFirst();
        
        // Configurer le slider de zoom
        sliderZoom.valueProperty().addListener((obs, old, newVal) -> {
            lblZoomLevel.setText(String.format("Zoom: %.1fx", newVal.doubleValue()));
        });
        
        // Charger les données initiales
        chargerDonnees();
        
        // Mettre à jour les statistiques
        mettreAJourStatistiques();
        
        // Indicateur de connexion (toujours offline)
        lblConnexion.setStyle("-fx-text-fill: #27ae60;");
    }
    
    private void chargerFiltres() {
        try {
            // Charger les pays
            List<String> pays = new ArrayList<>(Set.of(
                "Cameroun", "France", "Allemagne", "États-Unis", "Canada", 
                "Royaume-Uni", "Belgique", "Suisse", "Sénégal", "Côte d'Ivoire"
            ));
            cmbPays.getItems().addAll(pays);
            
            // Charger les spécialités
            List<String> specialites = stagiaireService.getToutesSpecialites();
            cmbSpecialite.getItems().addAll(specialites);
            
            // Charger les langues
            List<String> langues = stagiaireService.getToutesLangues();
            cmbLangue.getItems().addAll(langues);
            
        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur lors du chargement des filtres", e.getMessage());
        }
    }
    
    private void chargerDonnees() {
        progressLoading.setVisible(true);
        lblMapInfo.setText("Chargement des données...");
        
        new Thread(() -> {
            try {
                String typeAffichage = cmbTypeAffichage.getValue();
                String pays = cmbPays.getValue();
                String specialite = cmbSpecialite.getValue();
                String langue = cmbLangue.getValue();
                
                // Charger selon les filtres
                if (typeAffichage != null && typeAffichage.contains("Écoles")) {
                    chargerEcoles(typeAffichage, pays, specialite);
                } else if (typeAffichage != null && typeAffichage.contains("Stagiaires")) {
                    chargerStagiaires(pays, specialite, langue);
                } else {
                    // Tout afficher
                    chargerTout();
                }
                
                Platform.runLater(() -> {
                    progressLoading.setVisible(false);
                    mettreAJourStatistiques();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressLoading.setVisible(false);
                    afficherErreur("Erreur de chargement", e.getMessage());
                });
            }
        }).start();
    }
    
    private void chargerTout() {
        carte.clearMarkers();
        
        // Charger toutes les écoles
        ecolesCourantes = ecoleService.getToutesEcoles();
        for (Ecole ecole : ecolesCourantes) {
            if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                Color couleur = getColorForEcole(ecole);
                carte.addMarker(
                    ecole.getLatitude(),
                    ecole.getLongitude(),
                    ecole.getNom(),
                    "ecole",
                    ecole.getId(),
                    couleur
                );
            }
        }
        
        // Charger tous les stagiaires
        stagiairesCourants = stagiaireService.getStagiairesActifs();
        for (Stagiaire stagiaire : stagiairesCourants) {
            StageFormation stage = stageService.getStageActifByStagiaire(stagiaire.getId());
            if (stage != null && stage.getEcole() != null) {
                Ecole ecole = stage.getEcole();
                if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                    carte.addMarker(
                        ecole.getLatitude(),
                        ecole.getLongitude(),
                        stagiaire.getNomComplet(),
                        "stagiaire",
                        stagiaire.getId(),
                        Color.web("#3498db")
                    );
                }
            }
        }
        
        Platform.runLater(() -> {
            lblMapInfo.setText(String.format("%d écoles, %d stagiaires affichés", 
                ecolesCourantes.size(), stagiairesCourants.size()));
        });
    }
    
    private void chargerEcoles(String typeAffichage, String pays, String specialite) {
        carte.clearMarkers();
        
        List<Ecole> ecoles = ecoleService.getToutesEcoles();
        
        // Filtrer selon le type
        if (typeAffichage.contains("Professionnelles")) {
            ecoles = ecoles.stream()
                .filter(e -> "Professionnelle".equals(e.getType()))
                .collect(Collectors.toList());
        } else if (typeAffichage.contains("Académiques")) {
            ecoles = ecoles.stream()
                .filter(e -> "Académique".equals(e.getType()))
                .collect(Collectors.toList());
        } else if (typeAffichage.contains("Partenaires")) {
            ecoles = ecoles.stream()
                .filter(Ecole::isPartenaire)
                .collect(Collectors.toList());
        }
        
        // Filtrer par pays
        if (pays != null && !pays.isEmpty()) {
            final String paysFinal = pays;
            ecoles = ecoles.stream()
                .filter(e -> paysFinal.equals(e.getPays()))
                .collect(Collectors.toList());
        }
        
        // Filtrer par spécialité
        if (specialite != null && !specialite.isEmpty()) {
            final String specialiteFinal = specialite;
            ecoles = ecoles.stream()
                .filter(e -> e.getSpecialites() != null && 
                           e.getSpecialites().contains(specialiteFinal))
                .collect(Collectors.toList());
        }
        
        ecolesCourantes = ecoles;
        stagiairesCourants.clear();
        
        // Ajouter les marqueurs
        for (Ecole ecole : ecoles) {
            if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                Color couleur = getColorForEcole(ecole);
                carte.addMarker(
                    ecole.getLatitude(),
                    ecole.getLongitude(),
                    ecole.getNom(),
                    "ecole",
                    ecole.getId(),
                    couleur
                );
            }
        }
        
        Platform.runLater(() -> {
            lblMapInfo.setText(String.format("%d écoles affichées", ecoles.size()));
        });
    }
    
    private void chargerStagiaires(String pays, String specialite, String langue) {
        carte.clearMarkers();
        
        List<Stagiaire> stagiaires = stagiaireService.getStagiairesActifs();
        
        // Filtrer par spécialité
        if (specialite != null && !specialite.isEmpty()) {
            final String specialiteFinal = specialite;
            stagiaires = stagiaires.stream()
                .filter(s -> specialiteFinal.equals(s.getSpecialite()))
                .collect(Collectors.toList());
        }
        
        // Filtrer par langue
        if (langue != null && !langue.isEmpty()) {
            final String langueFinal = langue;
            stagiaires = stagiaires.stream()
                .filter(s -> langueFinal.equals(s.getLangue()))
                .collect(Collectors.toList());
        }
        
        stagiairesCourants = stagiaires;
        ecolesCourantes.clear();
        
        // Ajouter les marqueurs
        for (Stagiaire stagiaire : stagiaires) {
            StageFormation stage = stageService.getStageActifByStagiaire(stagiaire.getId());
            if (stage != null && stage.getEcole() != null) {
                Ecole ecole = stage.getEcole();
                
                // Filtrer par pays si nécessaire
                if (pays != null && !pays.isEmpty() && !pays.equals(ecole.getPays())) {
                    continue;
                }
                
                if (ecole.getLatitude() != null && ecole.getLongitude() != null) {
                    carte.addMarker(
                        ecole.getLatitude(),
                        ecole.getLongitude(),
                        stagiaire.getNomComplet(),
                        "stagiaire",
                        stagiaire.getId(),
                        Color.web("#3498db")
                    );
                }
            }
        }
        
        Platform.runLater(() -> {
            lblMapInfo.setText(String.format("%d stagiaires affichés", stagiaires.size()));
        });
    }
    
    private Color getColorForEcole(Ecole ecole) {
        if (ecole.getType() == null) return Color.web("#e74c3c");
        
        switch (ecole.getType()) {
            case "Professionnelle":
                return Color.web("#27ae60");
            case "Académique":
                return Color.web("#f39c12");
            default:
                return ecole.isPartenaire() ? Color.web("#9b59b6") : Color.web("#e74c3c");
        }
    }
    
    private void mettreAJourStatistiques() {
        Platform.runLater(() -> {
            lblTotalEcoles.setText(String.valueOf(ecolesCourantes.size()));
            lblTotalStagiaires.setText(String.valueOf(stagiairesCourants.size()));
        });
    }
    
    private void handleMarkerClick(OfflineMapView.MapMarker marker) {
        Platform.runLater(() -> {
            if ("ecole".equals(marker.type)) {
                Ecole ecole = ecoleService.getEcoleParId(marker.id);
                if (ecole != null) {
                    afficherDetailsEcole(ecole);
                }
            } else if ("stagiaire".equals(marker.type)) {
                Stagiaire stagiaire = stagiaireService.getStagiaireParId(marker.id);
                if (stagiaire != null) {
                    afficherDetailsStagiaire(stagiaire);
                }
            }
        });
    }
    
    private void afficherDetailsEcole(Ecole ecole) {
        elementSelectionne = ecole;
        detailsContent.getChildren().clear();
        
        // Titre
        Label titre = new Label(ecole.getNom());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Badge du type
        Label badge = new Label(ecole.getType());
        badge.setStyle("-fx-background-color: " + getColorHexForEcole(ecole) + 
                      "; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;");
        
        HBox header = new HBox(10, titre, badge);
        header.setStyle("-fx-alignment: center-left;");
        
        // Informations
        VBox infos = new VBox(10);
        infos.getChildren().addAll(
            creerLigneInfo("📍 Pays", ecole.getPays()),
            creerLigneInfo("🏙️ Ville", ecole.getVille()),
            creerLigneInfo("📧 Email", ecole.getEmail()),
            creerLigneInfo("📞 Téléphone", ecole.getTelephone()),
            creerLigneInfo("👤 Contact", ecole.getContact()),
            creerLigneInfo("🌐 Site Web", ecole.getSiteWeb())
        );
        
        // Statistiques de l'école
        int nbStagiaires = stagiaireService.compterStagiairesParEcole(ecole.getId());
        
        VBox stats = new VBox(5);
        stats.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 5;");
        
        Label lblStats = new Label("📊 Statistiques");
        lblStats.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label lblNbStagiaires = new Label("Stagiaires actuels: " + nbStagiaires);
        lblNbStagiaires.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498db;");
        
        stats.getChildren().addAll(lblStats, lblNbStagiaires);
        
        detailsContent.getChildren().addAll(
            header,
            new Separator(),
            infos,
            new Separator(),
            stats
        );
        
        detailsPanel.setVisible(true);
    }
    
    private void afficherDetailsStagiaire(Stagiaire stagiaire) {
        elementSelectionne = stagiaire;
        detailsContent.getChildren().clear();
        
        // Titre
        Label titre = new Label(stagiaire.getNomComplet());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Badge matricule
        Label badge = new Label(stagiaire.getMatricule());
        badge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                      "-fx-padding: 5 10; -fx-background-radius: 3;");
        
        HBox header = new HBox(10, titre, badge);
        header.setStyle("-fx-alignment: center-left;");
        
        // Informations personnelles
        VBox infos = new VBox(10);
        infos.getChildren().addAll(
            creerLigneInfo("🎓 Spécialité", stagiaire.getSpecialite()),
            creerLigneInfo("🌐 Langue", stagiaire.getLangue()),
            creerLigneInfo("📧 Email", stagiaire.getEmail()),
            creerLigneInfo("📞 Téléphone", stagiaire.getTelephone()),
            creerLigneInfo("🎯 Type Formation", stagiaire.getTypeFormation())
        );
        
        // Stage actif
        StageFormation stage = stageService.getStageActifByStagiaire(stagiaire.getId());
        
        VBox stageInfo = new VBox(10);
        stageInfo.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 15; -fx-background-radius: 5;");
        
        Label lblStage = new Label("📋 Stage Actif");
        lblStage.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        if (stage != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            stageInfo.getChildren().addAll(
                lblStage,
                creerLigneInfo("🏫 École", stage.getEcole().getNom()),
                creerLigneInfo("📍 Pays", stage.getEcole().getPays()),
                creerLigneInfo("📅 Début", stage.getDateDebut().format(formatter)),
                creerLigneInfo("📅 Fin", stage.getDateFin().format(formatter)),
                creerLigneInfo("⏱️ Statut", stage.getStatut())
            );
        } else {
            stageInfo.getChildren().addAll(
                lblStage,
                new Label("Aucun stage actif")
            );
        }
        
        detailsContent.getChildren().addAll(
            header,
            new Separator(),
            infos,
            new Separator(),
            stageInfo
        );
        
        detailsPanel.setVisible(true);
    }
    
    private HBox creerLigneInfo(String label, String valeur) {
        HBox ligne = new HBox(10);
        ligne.setStyle("-fx-alignment: center-left;");
        
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 120;");
        
        Label lblValeur = new Label(valeur != null ? valeur : "N/A");
        lblValeur.setWrapText(true);
        lblValeur.setStyle("-fx-text-fill: #555;");
        
        ligne.getChildren().addAll(lblLabel, lblValeur);
        return ligne;
    }
    
    private String getColorHexForEcole(Ecole ecole) {
        Color color = getColorForEcole(ecole);
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    // Gestionnaires d'événements
    @FXML
    private void handleFiltreChange() {
        chargerDonnees();
    }
    
    @FXML
    private void handleRechercher() {
        chargerDonnees();
    }
    
    @FXML
    private void handleReinitialiser() {
        cmbTypeAffichage.getSelectionModel().selectFirst();
        cmbPays.getSelectionModel().clearSelection();
        cmbSpecialite.getSelectionModel().clearSelection();
        cmbLangue.getSelectionModel().clearSelection();
        chargerDonnees();
    }
    
    @FXML
    private void handleExporter() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText("Exportation des données");
        alert.setContentText("Fonctionnalité d'export en développement.\n" +
                           "Formats prévus: PDF, Excel, CSV");
        alert.showAndWait();
    }
    
    @FXML
    private void zoomMonde() {
        carte.centerOn(20, 0, 0.8);
        sliderZoom.setValue(0.8);
    }
    
    @FXML
    private void zoomAfrique() {
        carte.centerOn(0, 20, 1.5);
        sliderZoom.setValue(1.5);
    }
    
    @FXML
    private void zoomCameroun() {
        carte.centerOn(7.3697, 12.3547, 4.0);
        sliderZoom.setValue(4.0);
    }
    
    @FXML
    private void zoomIn() {
        double newZoom = sliderZoom.getValue() * 1.2;
        sliderZoom.setValue(Math.min(newZoom, 10.0));
        handleZoomSlider();
    }
    
    @FXML
    private void zoomOut() {
        double newZoom = sliderZoom.getValue() / 1.2;
        sliderZoom.setValue(Math.max(newZoom, 0.5));
        handleZoomSlider();
    }
    
    @FXML
    private void handleZoomSlider() {
        double zoom = sliderZoom.getValue();
        carte.centerOn(carte.centerLat, carte.centerLon, zoom);
    }
    
    @FXML
    private void handleFermerDetails() {
        detailsPanel.setVisible(false);
        elementSelectionne = null;
    }
    
    @FXML
    private void handleModifier() {
        if (elementSelectionne == null) return;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Modification");
        alert.setHeaderText("Modifier l'élément");
        alert.setContentText("Fonctionnalité de modification en développement.");
        alert.showAndWait();
    }
    
    @FXML
    private void handleSupprimer() {
        if (elementSelectionne == null) return;
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'élément");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cet élément ?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implémenter la suppression
                afficherInfo("Suppression", "Élément supprimé avec succès.");
                handleFermerDetails();
                chargerDonnees();
            }
        });
    }
    
    @FXML
    private void handleGenererRapport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rapport");
        alert.setHeaderText("Génération de rapport");
        alert.setContentText("Fonctionnalité de génération de rapport en développement.");
        alert.showAndWait();
    }
    
    // Méthodes utilitaires
    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}