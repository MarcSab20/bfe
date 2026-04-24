package application.stages;

import application.ecoles.Ecole;
import application.ecoles.EcoleService;
import application.stagiaires.Stagiaire;
import application.stagiaires.StagiaireService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StagesViewController implements Initializable {

    // -- filtres & tableau -----------------------------------------
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private ComboBox<String> cmbType;
    @FXML private Label lblTotal;

    @FXML private TableView<StageFormation> tableStages;
    @FXML private TableColumn<StageFormation, String>  colStagiaire;
    @FXML private TableColumn<StageFormation, String>  colEcole;
    @FXML private TableColumn<StageFormation, String>  colType;
    @FXML private TableColumn<StageFormation, String>  colDebut;
    @FXML private TableColumn<StageFormation, String>  colFin;
    @FXML private TableColumn<StageFormation, String>  colStatut;
    @FXML private TableColumn<StageFormation, Long>    colJoursRestants;
    @FXML private TableColumn<StageFormation, Void>    colActions;

    // -- formulaire ------------------------------------------------
    @FXML private Label lblFormTitle;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnSupprimerForm;

    @FXML private ComboBox<Stagiaire> cmbStagiaire;
    @FXML private ComboBox<Ecole>     cmbEcole;
    @FXML private ComboBox<String>    cmbFormType;
    @FXML private ComboBox<String>    cmbFormStatut;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private TextField  txtSpecialite;
    @FXML private TextField  txtEncadrant;
    @FXML private TextField  txtTuteur;
    @FXML private TextArea   txtObjectifs;
    @FXML private TextArea   txtDescription;
    @FXML private TextArea   txtRemarques;
    @FXML private Label      lblDuree;

    // -- pagination ------------------------------------------------
    @FXML private Label lblPage;

    private final StageService     stageService     = new StageService();
    private final EcoleService     ecoleService     = new EcoleService();
    private final StagiaireService stagiaireService = new StagiaireService();

    private final ObservableList<StageFormation> stagesData     = FXCollections.observableArrayList();
    private final ObservableList<StageFormation> stagesFiltered = FXCollections.observableArrayList();

    private StageFormation stageSelectionne;
    private boolean modeEdition = false;
    private int currentPage = 0;
    private int pageSize    = 25;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerTableau();
        initialiserCombos();
        chargerDonnees();

        txtRecherche.textProperty().addListener((obs, old, n) -> appliquerFiltres());
        tableStages.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> {
            if (n != null && !modeEdition) afficherDetails(n);
        });

        // Calcul durée en temps réel
        if (dpDebut != null) dpDebut.valueProperty().addListener((o, a, n) -> majLblDuree());
        if (dpFin   != null) dpFin.valueProperty().addListener((o, a, n) -> majLblDuree());
    }

    private void initialiserCombos() {
        if (cmbStatut != null) cmbStatut.getItems().setAll("Tous", "En cours", "Terminé", "Annulé", "En attente");
        if (cmbStatut != null) cmbStatut.getSelectionModel().selectFirst();

        List<String> types = Arrays.asList("Stage académique", "Stage professionnel",
                                           "Mission", "Apprentissage", "Autre");
        if (cmbType     != null) { cmbType.getItems().setAll("Tous les types"); cmbType.getItems().addAll(types); cmbType.getSelectionModel().selectFirst(); }
        if (cmbFormType != null) cmbFormType.getItems().setAll(types);
        if (cmbFormStatut!=null) cmbFormStatut.getItems().setAll("En cours", "Terminé", "Annulé", "En attente");

        new Thread(() -> {
            List<Stagiaire> stagiaires = stagiaireService.getStagiairesActifs();
            List<Ecole>     ecoles     = ecoleService.getToutesEcoles();
            Platform.runLater(() -> {
                if (cmbStagiaire != null) {
                    cmbStagiaire.getItems().setAll(stagiaires);
                    cmbStagiaire.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(Stagiaire s) { return s != null ? s.getNomComplet() + " (" + s.getMatricule() + ")" : ""; }
                        @Override public Stagiaire fromString(String s) { return null; }
                    });
                }
                if (cmbEcole != null) {
                    cmbEcole.getItems().setAll(ecoles);
                    cmbEcole.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(Ecole e) { return e != null ? e.getNom() + " – " + e.getPays() : ""; }
                        @Override public Ecole fromString(String s) { return null; }
                    });
                }
            });
        }).start();
    }

    private void configurerTableau() {
        if (colStagiaire != null) colStagiaire.setCellValueFactory(cell -> {
            Stagiaire s = cell.getValue().getStagiaire();
            return new javafx.beans.property.SimpleStringProperty(s != null ? s.getNomComplet() : "");
        });
        if (colEcole  != null) colEcole.setCellValueFactory(cell -> {
            Ecole e = cell.getValue().getEcole();
            return new javafx.beans.property.SimpleStringProperty(e != null ? e.getNom() : "");
        });
        if (colType   != null) colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (colDebut  != null) colDebut.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDateDebut() != null ? cell.getValue().getDateDebut().format(FMT) : ""));
        if (colFin    != null) colFin.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDateFin() != null ? cell.getValue().getDateFin().format(FMT) : ""));
        if (colStatut != null) colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        if (colJoursRestants != null) colJoursRestants.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getJoursRestants()));

        // Coloration ligne selon urgence
        tableStages.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StageFormation sf, boolean empty) {
                super.updateItem(sf, empty);
                getStyleClass().removeAll("row-urgent", "row-warning", "row-termine");
                if (!empty && sf != null) {
                    if (sf.isTermine()) getStyleClass().add("row-termine");
                    else if (sf.isUrgent(7))  getStyleClass().add("row-urgent");
                    else if (sf.isUrgent(30)) getStyleClass().add("row-warning");
                }
            }
        });

        if (colActions != null) colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnMod = new Button("✏");
            private final Button btnDel = new Button("✕");
            private final HBox   box    = new HBox(5, btnMod, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnMod.getStyleClass().add("btn-action-small");
                btnDel.getStyleClass().add("btn-danger-small");
                btnMod.setOnAction(e -> modifierStage(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableStages.setItems(stagesFiltered);
    }

    private void chargerDonnees() {
        new Thread(() -> {
            try {
                List<StageFormation> list = stageService.getTousLesStages();
                Platform.runLater(() -> {
                    stagesData.setAll(list);
                    appliquerFiltres();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> afficherErreur("Erreur", e.getMessage()));
            }
        }).start();
    }

    private void appliquerFiltres() {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String statut = cmbStatut != null ? cmbStatut.getValue() : "Tous";
        String type   = cmbType   != null ? cmbType.getValue()   : "Tous les types";

        List<StageFormation> filtered = stagesData.stream().filter(sf -> {
            if (!recherche.isEmpty()) {
                String nom   = sf.getStagiaire() != null ? sf.getStagiaire().getNomComplet().toLowerCase() : "";
                String ecole = sf.getEcole()     != null ? sf.getEcole().getNom().toLowerCase() : "";
                if (!nom.contains(recherche) && !ecole.contains(recherche)) return false;
            }
            if (statut != null && !statut.equals("Tous") && !statut.equals(sf.getStatut())) return false;
            if (type   != null && !type.equals("Tous les types") && !type.equals(sf.getType())) return false;
            return true;
        }).collect(Collectors.toList());

        stagesFiltered.setAll(filtered);
        if (lblTotal != null) lblTotal.setText(filtered.size() + " stage(s)");
        currentPage = 0;
        mettreAJourPagination();
    }

    private void mettreAJourPagination() {
        if (lblPage == null) return;
        int total = (int) Math.ceil((double) stagesFiltered.size() / pageSize);
        lblPage.setText("Page " + (currentPage + 1) + " / " + Math.max(1, total));
    }

    private void majLblDuree() {
        if (lblDuree == null || dpDebut == null || dpFin == null) return;
        LocalDate debut = dpDebut.getValue();
        LocalDate fin   = dpFin.getValue();
        if (debut != null && fin != null && !fin.isBefore(debut)) {
            long jours = java.time.temporal.ChronoUnit.DAYS.between(debut, fin);
            long mois  = java.time.temporal.ChronoUnit.MONTHS.between(debut, fin);
            lblDuree.setText("Durée : " + jours + " jours (" + mois + " mois)");
        } else {
            lblDuree.setText("Durée : —");
        }
    }

    private void afficherDetails(StageFormation sf) {
        stageSelectionne = sf;
        if (lblFormTitle != null) lblFormTitle.setText("Détails du Stage");
        setVisibilite(false, false, false);
        remplirFormulaire(sf);
        setDisableFormulaire(true);
    }

    private void modifierStage(StageFormation sf) {
        stageSelectionne = sf;
        modeEdition = true;
        if (lblFormTitle != null) lblFormTitle.setText("Modifier le Stage");
        setVisibilite(true, true, true);
        remplirFormulaire(sf);
        setDisableFormulaire(false);
    }

    private void remplirFormulaire(StageFormation sf) {
        if (cmbStagiaire  != null) cmbStagiaire.setValue(sf.getStagiaire());
        if (cmbEcole      != null) cmbEcole.setValue(sf.getEcole());
        if (cmbFormType   != null) cmbFormType.setValue(sf.getType());
        if (cmbFormStatut != null) cmbFormStatut.setValue(sf.getStatut());
        if (dpDebut       != null) dpDebut.setValue(sf.getDateDebut());
        if (dpFin         != null) dpFin.setValue(sf.getDateFin());
        if (txtSpecialite != null) txtSpecialite.setText(nvl(sf.getSpecialite()));
        if (txtEncadrant  != null) txtEncadrant.setText(nvl(sf.getEncadrant()));
        if (txtTuteur     != null) txtTuteur.setText(nvl(sf.getTuteur()));
        if (txtObjectifs  != null) txtObjectifs.setText(nvl(sf.getObjectifs()));
        if (txtDescription!= null) txtDescription.setText(nvl(sf.getDescription()));
        if (txtRemarques  != null) txtRemarques.setText(nvl(sf.getRemarques()));
        majLblDuree();
    }

    private void viderFormulaire() {
        if (cmbStagiaire  != null) cmbStagiaire.getSelectionModel().clearSelection();
        if (cmbEcole      != null) cmbEcole.getSelectionModel().clearSelection();
        if (cmbFormType   != null) cmbFormType.getSelectionModel().clearSelection();
        if (cmbFormStatut != null) cmbFormStatut.getSelectionModel().selectFirst();
        if (dpDebut       != null) dpDebut.setValue(null);
        if (dpFin         != null) dpFin.setValue(null);
        if (txtSpecialite != null) txtSpecialite.clear();
        if (txtEncadrant  != null) txtEncadrant.clear();
        if (txtTuteur     != null) txtTuteur.clear();
        if (txtObjectifs  != null) txtObjectifs.clear();
        if (txtDescription!= null) txtDescription.clear();
        if (txtRemarques  != null) txtRemarques.clear();
        if (lblDuree      != null) lblDuree.setText("Durée : —");
    }

    private void setDisableFormulaire(boolean d) {
        if (cmbStagiaire  != null) cmbStagiaire.setDisable(d);
        if (cmbEcole      != null) cmbEcole.setDisable(d);
        if (cmbFormType   != null) cmbFormType.setDisable(d);
        if (cmbFormStatut != null) cmbFormStatut.setDisable(d);
        if (dpDebut       != null) dpDebut.setDisable(d);
        if (dpFin         != null) dpFin.setDisable(d);
        if (txtSpecialite != null) txtSpecialite.setDisable(d);
        if (txtEncadrant  != null) txtEncadrant.setDisable(d);
        if (txtTuteur     != null) txtTuteur.setDisable(d);
        if (txtObjectifs  != null) txtObjectifs.setDisable(d);
        if (txtDescription!= null) txtDescription.setDisable(d);
        if (txtRemarques  != null) txtRemarques.setDisable(d);
    }

    private void setVisibilite(boolean annuler, boolean enreg, boolean suppr) {
        if (btnAnnuler      != null) btnAnnuler.setVisible(annuler);
        if (btnEnregistrer  != null) btnEnregistrer.setVisible(enreg);
        if (btnSupprimerForm!= null) btnSupprimerForm.setVisible(suppr);
    }

    // ------------------------------------------------------------------ actions FXML

    @FXML
    private void handleNouveauStage() {
        stageSelectionne = null;
        modeEdition = true;
        if (lblFormTitle != null) lblFormTitle.setText("Nouveau Stage");
        setVisibilite(true, true, false);
        viderFormulaire();
        setDisableFormulaire(false);
        if (cmbFormStatut != null) cmbFormStatut.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleEnregistrer() {
        if (!valider()) return;

        StageFormation sf = stageSelectionne != null ? stageSelectionne : new StageFormation();
        lireFormulaire(sf);

        boolean ok;
        if (stageSelectionne != null && stageSelectionne.getId() != null) {
            ok = stageService.mettreAJourStage(sf);
        } else {
            sf.setDateCreation(LocalDate.now());
            ok = stageService.creerStage(sf);
        }

        if (ok) {
            afficherInfo("Succès", "Stage enregistré avec succès.");
            chargerDonnees();
            handleAnnuler();
        } else {
            afficherErreur("Erreur", "Échec de l'enregistrement.");
        }
    }

    private void lireFormulaire(StageFormation sf) {
        if (cmbStagiaire  != null) sf.setStagiaire(cmbStagiaire.getValue());
        if (cmbEcole      != null) sf.setEcole(cmbEcole.getValue());
        if (cmbFormType   != null) sf.setType(cmbFormType.getValue());
        if (cmbFormStatut != null) sf.setStatut(cmbFormStatut.getValue());
        if (dpDebut       != null) sf.setDateDebut(dpDebut.getValue());
        if (dpFin         != null) sf.setDateFin(dpFin.getValue());
        if (txtSpecialite != null) sf.setSpecialite(txtSpecialite.getText().trim());
        if (txtEncadrant  != null) sf.setEncadrant(txtEncadrant.getText().trim());
        if (txtTuteur     != null) sf.setTuteur(txtTuteur.getText().trim());
        if (txtObjectifs  != null) sf.setObjectifs(txtObjectifs.getText().trim());
        if (txtDescription!= null) sf.setDescription(txtDescription.getText().trim());
        if (txtRemarques  != null) sf.setRemarques(txtRemarques.getText().trim());
    }

    private boolean valider() {
        if (cmbStagiaire == null || cmbStagiaire.getValue() == null) {
            afficherErreur("Validation", "Veuillez sélectionner un stagiaire."); return false;
        }
        if (cmbEcole == null || cmbEcole.getValue() == null) {
            afficherErreur("Validation", "Veuillez sélectionner une école."); return false;
        }
        if (dpDebut == null || dpDebut.getValue() == null) {
            afficherErreur("Validation", "La date de début est requise."); return false;
        }
        if (dpFin == null || dpFin.getValue() == null) {
            afficherErreur("Validation", "La date de fin est requise."); return false;
        }
        if (dpFin.getValue().isBefore(dpDebut.getValue())) {
            afficherErreur("Validation", "La date de fin doit être après la date de début."); return false;
        }
        return true;
    }

    @FXML
    private void handleSupprimerForm() {
        if (stageSelectionne != null) confirmerSuppression(stageSelectionne);
    }

    private void confirmerSuppression(StageFormation sf) {
        String nom = sf.getStagiaire() != null ? sf.getStagiaire().getNomComplet() : "ce stage";
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le stage de « " + nom + " » ?", ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmation"); a.setHeaderText(null);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (stageService.supprimerStage(sf.getId())) {
                    afficherInfo("Suppression", "Stage supprimé.");
                    chargerDonnees();
                    handleAnnuler();
                } else {
                    afficherErreur("Erreur", "Échec de la suppression.");
                }
            }
        });
    }

    @FXML
    private void handleAnnuler() {
        modeEdition = false;
        stageSelectionne = null;
        if (lblFormTitle != null) lblFormTitle.setText("Détails du Stage");
        setVisibilite(false, false, false);
        viderFormulaire();
        setDisableFormulaire(true);
    }

    @FXML private void handleFiltreChange()   { appliquerFiltres(); }
    @FXML private void handleReinitialiser()  {
        txtRecherche.clear();
        if (cmbStatut != null) cmbStatut.getSelectionModel().selectFirst();
        if (cmbType   != null) cmbType.getSelectionModel().selectFirst();
        appliquerFiltres();
    }
    @FXML private void handleExporter() { afficherInfo("Export", "Export en développement."); }

    @FXML private void handlePagePrecedente() {
        if (currentPage > 0) { currentPage--; mettreAJourPagination(); }
    }
    @FXML private void handlePageSuivante() {
        int total = (int) Math.ceil((double) stagesFiltered.size() / pageSize);
        if (currentPage < total - 1) { currentPage++; mettreAJourPagination(); }
    }

    // ------------------------------------------------------------------ utilitaires

    private String nvl(String s) { return s != null ? s : ""; }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg); a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }
    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg); a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }
}
