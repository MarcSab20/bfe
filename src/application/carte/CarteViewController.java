package application.carte;

import application.ecoles.Ecole;
import application.ecoles.EcoleService;
import application.stagiaires.Stagiaire;
import application.stagiaires.StagiaireService;
import application.stages.StageFormation;
import application.stages.StageService;

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

    private final EcoleService     ecoleService     = new EcoleService();
    private final StagiaireService stagiaireService = new StagiaireService();
    private final StageService     stageService     = new StageService();

    private OfflineMapView carte;
    private List<Ecole>     ecolesCourantes     = new ArrayList<>();
    private List<Stagiaire> stagiairesCourants  = new ArrayList<>();
    private Object elementSelectionne;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        carte = new OfflineMapView();
        if (mapContainer != null) mapContainer.getChildren().add(carte);

        carte.setMarkerClickListener(this::handleMarkerClick);

        chargerFiltres();
        if (cmbTypeAffichage != null) cmbTypeAffichage.getSelectionModel().selectFirst();

        if (sliderZoom != null) {
            sliderZoom.valueProperty().addListener((obs, old, nv) ->
                { if (lblZoomLevel != null) lblZoomLevel.setText(String.format("Zoom: %.1fx", nv.doubleValue())); });
        }

        chargerDonnees();
        mettreAJourStatistiques();

        if (lblConnexion != null) lblConnexion.setStyle("-fx-text-fill: #27ae60;");
    }

    private void chargerFiltres() {
        try {
            if (cmbPays != null) {
                List<String> pays = new ArrayList<>(Arrays.asList(
                    "Cameroun","France","Allemagne","États-Unis","Canada",
                    "Royaume-Uni","Belgique","Suisse","Sénégal","Côte d'Ivoire"));
                cmbPays.getItems().addAll(pays);
            }
            if (cmbSpecialite != null) cmbSpecialite.getItems().addAll(stagiaireService.getToutesSpecialites());
            if (cmbLangue     != null) cmbLangue.getItems().addAll(stagiaireService.getToutesLangues());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void chargerDonnees() {
        if (progressLoading != null) progressLoading.setVisible(true);
        if (lblMapInfo != null) lblMapInfo.setText("Chargement…");

        new Thread(() -> {
            try {
                String type = cmbTypeAffichage != null ? cmbTypeAffichage.getValue() : null;
                String pays = cmbPays      != null ? cmbPays.getValue()      : null;
                String spec = cmbSpecialite!= null ? cmbSpecialite.getValue(): null;
                String lang = cmbLangue    != null ? cmbLangue.getValue()    : null;

                if (type != null && type.contains("Stagiaires")) chargerStagiaires(pays, spec, lang);
                else if (type != null && type.contains("Écoles")) chargerEcoles(type, pays, spec);
                else chargerTout();

                Platform.runLater(() -> {
                    if (progressLoading != null) progressLoading.setVisible(false);
                    mettreAJourStatistiques();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { if (progressLoading != null) progressLoading.setVisible(false); });
            }
        }).start();
    }

    private void chargerTout() {
        carte.clearMarkers();
        ecolesCourantes = ecoleService.getToutesEcoles();
        for (Ecole ecole : ecolesCourantes) {
            if (ecole.getLatitude() != null && ecole.getLongitude() != null)
                carte.addMarker(ecole.getLatitude(), ecole.getLongitude(),
                    ecole.getNom(), "ecole", ecole.getId(), couleurEcole(ecole));
        }
        stagiairesCourants = stagiaireService.getStagiairesActifs();
        for (Stagiaire s : stagiairesCourants) {
            StageFormation sf = stageService.getStageActifByStagiaire(s.getId());
            if (sf != null && sf.getEcole() != null
                    && sf.getEcole().getLatitude() != null && sf.getEcole().getLongitude() != null)
                carte.addMarker(sf.getEcole().getLatitude(), sf.getEcole().getLongitude(),
                    s.getNomComplet(), "stagiaire", s.getId(), Color.web("#3498db"));
        }
        int ne = ecolesCourantes.size(), ns = stagiairesCourants.size();
        Platform.runLater(() -> { if (lblMapInfo != null) lblMapInfo.setText(ne + " écoles, " + ns + " stagiaires"); });
    }

    private void chargerEcoles(String typeAffichage, String pays, String specialite) {
        carte.clearMarkers();
        List<Ecole> ecoles = ecoleService.getToutesEcoles();

        if (typeAffichage.contains("Professionnelles"))
            ecoles = ecoles.stream().filter(e -> "Professionnelle".equals(e.getType())).collect(Collectors.toList());
        else if (typeAffichage.contains("Académiques"))
            ecoles = ecoles.stream().filter(e -> "Académique".equals(e.getType())).collect(Collectors.toList());
        else if (typeAffichage.contains("Partenaires"))
            ecoles = ecoles.stream().filter(Ecole::isPartenaire).collect(Collectors.toList());

        if (pays != null && !pays.isEmpty())
            ecoles = ecoles.stream().filter(e -> pays.equals(e.getPays())).collect(Collectors.toList());
        if (specialite != null && !specialite.isEmpty())
            ecoles = ecoles.stream().filter(e -> e.getSpecialites() != null && e.getSpecialites().contains(specialite))
                           .collect(Collectors.toList());

        ecolesCourantes = ecoles;
        stagiairesCourants.clear();

        for (Ecole e : ecoles)
            if (e.getLatitude() != null && e.getLongitude() != null)
                carte.addMarker(e.getLatitude(), e.getLongitude(), e.getNom(), "ecole", e.getId(), couleurEcole(e));

        int n = ecoles.size();
        Platform.runLater(() -> { if (lblMapInfo != null) lblMapInfo.setText(n + " école(s)"); });
    }

    private void chargerStagiaires(String pays, String specialite, String langue) {
        carte.clearMarkers();
        List<Stagiaire> stagiaires = stagiaireService.getStagiairesActifs();

        if (specialite != null && !specialite.isEmpty())
            stagiaires = stagiaires.stream().filter(s -> specialite.equals(s.getSpecialite())).collect(Collectors.toList());
        if (langue != null && !langue.isEmpty())
            stagiaires = stagiaires.stream().filter(s -> langue.equals(s.getLangue())).collect(Collectors.toList());

        stagiairesCourants = stagiaires;
        ecolesCourantes.clear();

        for (Stagiaire s : stagiaires) {
            StageFormation sf = stageService.getStageActifByStagiaire(s.getId());
            if (sf != null && sf.getEcole() != null) {
                Ecole e = sf.getEcole();
                if (pays != null && !pays.isEmpty() && !pays.equals(e.getPays())) continue;
                if (e.getLatitude() != null && e.getLongitude() != null)
                    carte.addMarker(e.getLatitude(), e.getLongitude(),
                        s.getNomComplet(), "stagiaire", s.getId(), Color.web("#3498db"));
            }
        }
        int n = stagiaires.size();
        Platform.runLater(() -> { if (lblMapInfo != null) lblMapInfo.setText(n + " stagiaire(s)"); });
    }

    private Color couleurEcole(Ecole e) {
        if (e.getType() == null) return Color.web("#e74c3c");
        return switch (e.getType()) {
            case "Professionnelle" -> Color.web("#27ae60");
            case "Académique"      -> Color.web("#f39c12");
            default -> e.isPartenaire() ? Color.web("#9b59b6") : Color.web("#e74c3c");
        };
    }

    private void mettreAJourStatistiques() {
        Platform.runLater(() -> {
            if (lblTotalEcoles     != null) lblTotalEcoles.setText(String.valueOf(ecolesCourantes.size()));
            if (lblTotalStagiaires != null) lblTotalStagiaires.setText(String.valueOf(stagiairesCourants.size()));
        });
    }

    private void handleMarkerClick(OfflineMapView.MapMarker marker) {
        Platform.runLater(() -> {
            if ("ecole".equals(marker.type)) {
                Ecole e = ecoleService.getEcoleParId(marker.id);
                if (e != null) afficherDetailsEcole(e);
            } else if ("stagiaire".equals(marker.type)) {
                Stagiaire s = stagiaireService.getStagiaireParId(marker.id);
                if (s != null) afficherDetailsStagiaire(s);
            }
        });
    }

    private void afficherDetailsEcole(Ecole ecole) {
        elementSelectionne = ecole;
        if (detailsContent == null) return;
        detailsContent.getChildren().clear();

        Label titre = new Label(ecole.getNom());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label badge = new Label(ecole.getType() != null ? ecole.getType() : "");
        badge.setStyle("-fx-background-color: " + couleurHex(ecole) +
                       "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 4;");

        HBox header = new HBox(10, titre, badge);

        VBox infos = new VBox(8,
            ligne("Pays", ecole.getPays()), ligne("Ville", ecole.getVille()),
            ligne("Email", ecole.getEmail()), ligne("Tél.", ecole.getTelephone()),
            ligne("Contact", ecole.getContact()), ligne("Site", ecole.getSiteWeb()));

        int nb = stagiaireService.compterStagiairesParEcole(ecole.getId());
        Label lblNb = new Label("Stagiaires en cours : " + nb);
        lblNb.setStyle("-fx-font-size: 15px; -fx-text-fill: #3498db; -fx-font-weight: bold;");

        detailsContent.getChildren().addAll(header, new Separator(), infos, new Separator(), lblNb);
        if (detailsPanel != null) detailsPanel.setVisible(true);
    }

    private void afficherDetailsStagiaire(Stagiaire stagiaire) {
        elementSelectionne = stagiaire;
        if (detailsContent == null) return;
        detailsContent.getChildren().clear();

        Label titre = new Label(stagiaire.getNomComplet());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label badge = new Label(stagiaire.getMatricule() != null ? stagiaire.getMatricule() : "");
        badge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 4;");

        HBox header = new HBox(10, titre, badge);

        VBox infos = new VBox(8,
            ligne("Spécialité", stagiaire.getSpecialite()),
            ligne("Langue", stagiaire.getLangue()),
            ligne("Email", stagiaire.getEmail()),
            ligne("Tél.", stagiaire.getTelephone()),
            ligne("Formation", stagiaire.getTypeFormation()));

        StageFormation sf = stageService.getStageActifByStagiaire(stagiaire.getId());
        VBox stageBox = new VBox(8);
        stageBox.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 10; -fx-background-radius: 5;");
        stageBox.getChildren().add(new Label("Stage actif"));

        if (sf != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            stageBox.getChildren().addAll(
                ligne("École", sf.getEcole() != null ? sf.getEcole().getNom() : "N/A"),
                ligne("Pays",  sf.getEcole() != null ? sf.getEcole().getPays() : "N/A"),
                ligne("Début", sf.getDateDebut() != null ? sf.getDateDebut().format(fmt) : "N/A"),
                ligne("Fin",   sf.getDateFin()   != null ? sf.getDateFin().format(fmt)   : "N/A"),
                ligne("Statut", sf.getStatut()));
        } else {
            stageBox.getChildren().add(new Label("Aucun stage actif"));
        }

        detailsContent.getChildren().addAll(header, new Separator(), infos, new Separator(), stageBox);
        if (detailsPanel != null) detailsPanel.setVisible(true);
    }

    private HBox ligne(String label, String valeur) {
        Label l = new Label(label + " :"); l.setStyle("-fx-font-weight: bold; -fx-min-width: 100;");
        Label v = new Label(valeur != null ? valeur : "N/A"); v.setWrapText(true);
        HBox h = new HBox(8, l, v); h.setPadding(new Insets(0));
        return h;
    }

    private String couleurHex(Ecole e) {
        Color c = couleurEcole(e);
        return String.format("#%02X%02X%02X",
            (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));
    }

    // ------------------------------------------------------------------ actions FXML

    @FXML private void handleFiltreChange()  { chargerDonnees(); }
    @FXML private void handleRechercher()    { chargerDonnees(); }

    @FXML private void handleReinitialiser() {
        if (cmbTypeAffichage != null) cmbTypeAffichage.getSelectionModel().selectFirst();
        if (cmbPays      != null) cmbPays.getSelectionModel().clearSelection();
        if (cmbSpecialite!= null) cmbSpecialite.getSelectionModel().clearSelection();
        if (cmbLangue    != null) cmbLangue.getSelectionModel().clearSelection();
        chargerDonnees();
    }

    @FXML private void handleExporter() { afficherInfo("Export", "Export (PDF / Excel / CSV) en développement."); }

    @FXML private void zoomMonde()    { carte.centerOn(20, 0, 0.8);      maj(0.8); }
    @FXML private void zoomAfrique()  { carte.centerOn(0, 20, 1.5);      maj(1.5); }
    @FXML private void zoomCameroun() { carte.centerOn(7.3697, 12.3547, 4.0); maj(4.0); }

    @FXML private void zoomIn() {
        double nv = sliderZoom != null ? Math.min(sliderZoom.getValue() * 1.2, 10.0) : 2.0;
        maj(nv); carte.setZoom(nv);
    }
    @FXML private void zoomOut() {
        double nv = sliderZoom != null ? Math.max(sliderZoom.getValue() / 1.2, 0.5) : 0.8;
        maj(nv); carte.setZoom(nv);
    }
    @FXML private void handleZoomSlider() {
        if (sliderZoom != null) carte.setZoom(sliderZoom.getValue());
    }

    private void maj(double zoom) {
        if (sliderZoom  != null) sliderZoom.setValue(zoom);
        if (lblZoomLevel!= null) lblZoomLevel.setText(String.format("Zoom: %.1fx", zoom));
    }

    @FXML private void handleFermerDetails() {
        if (detailsPanel != null) detailsPanel.setVisible(false);
        elementSelectionne = null;
    }

    @FXML private void handleModifier() {
        afficherInfo("Modification", "Modification via la fenêtre dédiée en développement.");
    }

    @FXML private void handleSupprimer() {
        if (elementSelectionne == null) return;
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cet élément ?", ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmation"); conf.setHeaderText(null);
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = false;
                if (elementSelectionne instanceof Ecole)
                    ok = new application.ecoles.EcoleService().supprimerEcole(((Ecole) elementSelectionne).getId());
                else if (elementSelectionne instanceof Stagiaire)
                    ok = new application.stagiaires.StagiaireService()
                            .supprimerStagiaire(((Stagiaire) elementSelectionne).getId());
                if (ok) { handleFermerDetails(); chargerDonnees(); }
            }
        });
    }

    @FXML private void handleGenererRapport() { afficherInfo("Rapport", "Génération de rapport en développement."); }

    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg); a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }
}
