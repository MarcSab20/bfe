package application.stagiaires;

import application.stages.StageFormation;
import application.stages.StageService;

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

public class StagiairesViewController implements Initializable {

    // -- tableau -------------------------------------------------------
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cmbSpecialite;
    @FXML private ComboBox<String> cmbLangue;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Label lblTotal;

    @FXML private TableView<Stagiaire> tableStagiaires;
    @FXML private TableColumn<Stagiaire, String>  colMatricule;
    @FXML private TableColumn<Stagiaire, String>  colNom;
    @FXML private TableColumn<Stagiaire, String>  colPrenom;
    @FXML private TableColumn<Stagiaire, String>  colSpecialite;
    @FXML private TableColumn<Stagiaire, String>  colLangue;
    @FXML private TableColumn<Stagiaire, String>  colStatut;
    @FXML private TableColumn<Stagiaire, Void>    colActions;

    // -- formulaire ----------------------------------------------------
    @FXML private Label lblFormTitle;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnSupprimerForm;

    @FXML private TextField txtMatricule;
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private DatePicker dpDateNaissance;
    @FXML private TextField txtLieuNaissance;
    @FXML private TextField txtNationalite;
    @FXML private ComboBox<String> cmbSexe;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private TextArea  txtAdresse;
    @FXML private ComboBox<String> cmbFormSpecialite;
    @FXML private ComboBox<String> cmbFormLangue;
    @FXML private ComboBox<String> cmbNiveauEtude;
    @FXML private TextField txtDiplome;
    @FXML private ComboBox<String> cmbTypeFormation;
    @FXML private TextArea  txtRemarques;

    // -- pagination ----------------------------------------------------
    @FXML private Label lblPage;
    @FXML private ComboBox<String> cmbPageSize;

    private final StagiaireService stagiaireService = new StagiaireService();
    private final StageService     stageService     = new StageService();

    private final ObservableList<Stagiaire> stagiairesData     = FXCollections.observableArrayList();
    private final ObservableList<Stagiaire> stagiairesFiltered = FXCollections.observableArrayList();

    private Stagiaire stagiaireSelectionne;
    private boolean modeEdition = false;
    private int currentPage = 0;
    private int pageSize    = 25;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerTableau();
        initialiserCombos();
        chargerDonnees();

        txtRecherche.textProperty().addListener((obs, old, n) -> appliquerFiltres());
        tableStagiaires.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> {
            if (n != null && !modeEdition) afficherDetails(n);
        });
    }

    private void initialiserCombos() {
        if (cmbSexe != null) cmbSexe.getItems().setAll("Masculin", "Féminin", "Autre");
        if (cmbNiveauEtude != null) cmbNiveauEtude.getItems().setAll(
            "Bac", "Bac+2", "Bac+3 (Licence)", "Bac+4", "Bac+5 (Master)", "Doctorat", "Autre");
        if (cmbTypeFormation != null) cmbTypeFormation.getItems().setAll(
            "Initiale", "Continue", "Alternance", "Professionnelle", "Autre");
        if (cmbStatut != null) cmbStatut.getItems().setAll("Tous", "Actifs", "Inactifs");
        if (cmbStatut != null) cmbStatut.getSelectionModel().selectFirst();

        new Thread(() -> {
            List<String> specialites = stagiaireService.getToutesSpecialites();
            List<String> langues     = stagiaireService.getToutesLangues();
            Platform.runLater(() -> {
                if (cmbSpecialite != null) { cmbSpecialite.getItems().setAll("Toutes"); cmbSpecialite.getItems().addAll(specialites); cmbSpecialite.getSelectionModel().selectFirst(); }
                if (cmbLangue     != null) { cmbLangue.getItems().setAll("Toutes");     cmbLangue.getItems().addAll(langues);          cmbLangue.getSelectionModel().selectFirst(); }
                if (cmbFormSpecialite != null) cmbFormSpecialite.getItems().addAll(specialites);
                if (cmbFormLangue     != null) cmbFormLangue.getItems().addAll(langues);
            });
        }).start();
    }

    private void configurerTableau() {
        if (colMatricule != null) colMatricule.setCellValueFactory(new PropertyValueFactory<>("matricule"));
        if (colNom       != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom    != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colSpecialite!= null) colSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));
        if (colLangue    != null) colLangue.setCellValueFactory(new PropertyValueFactory<>("langue"));

        if (colStatut != null) colStatut.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().isActif() ? "Actif" : "Inactif"));

        if (colActions != null) colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnMod = new Button("✏");
            private final Button btnDel = new Button("✕");
            private final HBox   box    = new HBox(5, btnMod, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnMod.getStyleClass().add("btn-action-small");
                btnDel.getStyleClass().add("btn-danger-small");
                btnMod.setOnAction(e -> modifierStagiaire(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableStagiaires.setItems(stagiairesFiltered);
    }

    private void chargerDonnees() {
        new Thread(() -> {
            try {
                List<Stagiaire> list = stagiaireService.getTousStagiaires();
                Platform.runLater(() -> {
                    stagiairesData.setAll(list);
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
        String spec   = cmbSpecialite != null ? cmbSpecialite.getValue() : null;
        String lang   = cmbLangue     != null ? cmbLangue.getValue()     : null;
        String statut = cmbStatut     != null ? cmbStatut.getValue()     : "Tous";

        List<Stagiaire> filtered = stagiairesData.stream().filter(s -> {
            if (!recherche.isEmpty()) {
                String haystack = (nvl(s.getNom()) + " " + nvl(s.getPrenom()) + " " + nvl(s.getMatricule())
                                 + " " + nvl(s.getEmail())).toLowerCase();
                if (!haystack.contains(recherche)) return false;
            }
            if (spec   != null && !spec.equals("Toutes") && !spec.equals(s.getSpecialite()))   return false;
            if (lang   != null && !lang.equals("Toutes") && !lang.equals(s.getLangue()))        return false;
            if ("Actifs".equals(statut)   && !s.isActif()) return false;
            if ("Inactifs".equals(statut) &&  s.isActif()) return false;
            return true;
        }).collect(Collectors.toList());

        stagiairesFiltered.setAll(filtered);
        if (lblTotal != null) lblTotal.setText(filtered.size() + " stagiaire(s)");
        currentPage = 0;
        mettreAJourPagination();
    }

    private void mettreAJourPagination() {
        if (lblPage == null) return;
        int total = (int) Math.ceil((double) stagiairesFiltered.size() / pageSize);
        lblPage.setText("Page " + (currentPage + 1) + " / " + Math.max(1, total));
    }

    private void afficherDetails(Stagiaire s) {
        stagiaireSelectionne = s;
        if (lblFormTitle != null) lblFormTitle.setText("Détails du Stagiaire");
        setVisibilite(false, false, false);
        remplirFormulaire(s);
        setDisableFormulaire(true);
    }

    private void modifierStagiaire(Stagiaire s) {
        stagiaireSelectionne = s;
        modeEdition = true;
        if (lblFormTitle != null) lblFormTitle.setText("Modifier le Stagiaire");
        setVisibilite(true, true, true);
        remplirFormulaire(s);
        setDisableFormulaire(false);
    }

    private void remplirFormulaire(Stagiaire s) {
        if (txtMatricule    != null) { txtMatricule.setText(nvl(s.getMatricule())); txtMatricule.setDisable(true); }
        if (txtNom          != null) txtNom.setText(nvl(s.getNom()));
        if (txtPrenom       != null) txtPrenom.setText(nvl(s.getPrenom()));
        if (dpDateNaissance != null) dpDateNaissance.setValue(s.getDateNaissance());
        if (txtLieuNaissance!= null) txtLieuNaissance.setText(nvl(s.getLieuNaissance()));
        if (txtNationalite  != null) txtNationalite.setText(nvl(s.getNationalite()));
        if (cmbSexe         != null) cmbSexe.setValue(s.getSexe());
        if (txtEmail        != null) txtEmail.setText(nvl(s.getEmail()));
        if (txtTelephone    != null) txtTelephone.setText(nvl(s.getTelephone()));
        if (txtAdresse      != null) txtAdresse.setText(nvl(s.getAdresse()));
        if (cmbFormSpecialite != null) cmbFormSpecialite.setValue(s.getSpecialite());
        if (cmbFormLangue   != null) cmbFormLangue.setValue(s.getLangue());
        if (cmbNiveauEtude  != null) cmbNiveauEtude.setValue(s.getNiveauEtude());
        if (txtDiplome      != null) txtDiplome.setText(nvl(s.getDiplome()));
        if (cmbTypeFormation!= null) cmbTypeFormation.setValue(s.getTypeFormation());
        if (txtRemarques    != null) txtRemarques.setText(nvl(s.getRemarques()));
    }

    private void viderFormulaire() {
        if (txtMatricule    != null) { txtMatricule.clear(); txtMatricule.setDisable(false); }
        if (txtNom          != null) txtNom.clear();
        if (txtPrenom       != null) txtPrenom.clear();
        if (dpDateNaissance != null) dpDateNaissance.setValue(null);
        if (txtLieuNaissance!= null) txtLieuNaissance.clear();
        if (txtNationalite  != null) txtNationalite.clear();
        if (cmbSexe         != null) cmbSexe.getSelectionModel().clearSelection();
        if (txtEmail        != null) txtEmail.clear();
        if (txtTelephone    != null) txtTelephone.clear();
        if (txtAdresse      != null) txtAdresse.clear();
        if (cmbFormSpecialite != null) cmbFormSpecialite.getSelectionModel().clearSelection();
        if (cmbFormLangue   != null) cmbFormLangue.getSelectionModel().clearSelection();
        if (cmbNiveauEtude  != null) cmbNiveauEtude.getSelectionModel().clearSelection();
        if (txtDiplome      != null) txtDiplome.clear();
        if (cmbTypeFormation!= null) cmbTypeFormation.getSelectionModel().clearSelection();
        if (txtRemarques    != null) txtRemarques.clear();
    }

    private void setDisableFormulaire(boolean d) {
        if (txtNom          != null) txtNom.setDisable(d);
        if (txtPrenom       != null) txtPrenom.setDisable(d);
        if (dpDateNaissance != null) dpDateNaissance.setDisable(d);
        if (txtLieuNaissance!= null) txtLieuNaissance.setDisable(d);
        if (txtNationalite  != null) txtNationalite.setDisable(d);
        if (cmbSexe         != null) cmbSexe.setDisable(d);
        if (txtEmail        != null) txtEmail.setDisable(d);
        if (txtTelephone    != null) txtTelephone.setDisable(d);
        if (txtAdresse      != null) txtAdresse.setDisable(d);
        if (cmbFormSpecialite!=null) cmbFormSpecialite.setDisable(d);
        if (cmbFormLangue   != null) cmbFormLangue.setDisable(d);
        if (cmbNiveauEtude  != null) cmbNiveauEtude.setDisable(d);
        if (txtDiplome      != null) txtDiplome.setDisable(d);
        if (cmbTypeFormation!= null) cmbTypeFormation.setDisable(d);
        if (txtRemarques    != null) txtRemarques.setDisable(d);
    }

    private void setVisibilite(boolean annuler, boolean enreg, boolean suppr) {
        if (btnAnnuler      != null) btnAnnuler.setVisible(annuler);
        if (btnEnregistrer  != null) btnEnregistrer.setVisible(enreg);
        if (btnSupprimerForm!= null) btnSupprimerForm.setVisible(suppr);
    }

    // ------------------------------------------------------------------ actions FXML

    @FXML
    private void handleNouveauStagiaire() {
        stagiaireSelectionne = null;
        modeEdition = true;
        if (lblFormTitle != null) lblFormTitle.setText("Nouveau Stagiaire");
        setVisibilite(true, true, false);
        viderFormulaire();
        setDisableFormulaire(false);
        if (txtMatricule != null) {
            String mat = stagiaireService.genererMatricule();
            txtMatricule.setText(mat);
            txtMatricule.setDisable(true);
        }
    }

    @FXML
    private void handleEnregistrer() {
        if (!valider()) return;

        Stagiaire s = stagiaireSelectionne != null ? stagiaireSelectionne : new Stagiaire();
        lireFormulaire(s);

        boolean ok;
        if (stagiaireSelectionne != null && stagiaireSelectionne.getId() != null) {
            ok = stagiaireService.mettreAJourStagiaire(s);
        } else {
            s.setActif(true);
            s.setDateInscription(LocalDate.now());
            ok = stagiaireService.creerStagiaire(s);
        }

        if (ok) {
            afficherInfo("Succès", "Stagiaire enregistré avec succès.");
            chargerDonnees();
            handleAnnuler();
        } else {
            afficherErreur("Erreur", "Échec de l'enregistrement.");
        }
    }

    private void lireFormulaire(Stagiaire s) {
        if (txtMatricule    != null) s.setMatricule(txtMatricule.getText().trim());
        if (txtNom          != null) s.setNom(txtNom.getText().trim());
        if (txtPrenom       != null) s.setPrenom(txtPrenom.getText().trim());
        if (dpDateNaissance != null) s.setDateNaissance(dpDateNaissance.getValue());
        if (txtLieuNaissance!= null) s.setLieuNaissance(txtLieuNaissance.getText().trim());
        if (txtNationalite  != null) s.setNationalite(txtNationalite.getText().trim());
        if (cmbSexe         != null) s.setSexe(cmbSexe.getValue());
        if (txtEmail        != null) s.setEmail(txtEmail.getText().trim());
        if (txtTelephone    != null) s.setTelephone(txtTelephone.getText().trim());
        if (txtAdresse      != null) s.setAdresse(txtAdresse.getText().trim());
        if (cmbFormSpecialite!=null) s.setSpecialite(cmbFormSpecialite.getValue());
        if (cmbFormLangue   != null) s.setLangue(cmbFormLangue.getValue());
        if (cmbNiveauEtude  != null) s.setNiveauEtude(cmbNiveauEtude.getValue());
        if (txtDiplome      != null) s.setDiplome(txtDiplome.getText().trim());
        if (cmbTypeFormation!= null) s.setTypeFormation(cmbTypeFormation.getValue());
        if (txtRemarques    != null) s.setRemarques(txtRemarques.getText().trim());
    }

    private boolean valider() {
        if (txtNom   == null || txtNom.getText().trim().isEmpty())    { afficherErreur("Validation", "Le nom est requis.");   return false; }
        if (txtPrenom== null || txtPrenom.getText().trim().isEmpty())  { afficherErreur("Validation", "Le prénom est requis."); return false; }
        if (txtEmail != null && !txtEmail.getText().isEmpty() && !txtEmail.getText().contains("@")) {
            afficherErreur("Validation", "L'email n'est pas valide."); return false;
        }
        return true;
    }

    @FXML
    private void handleSupprimerForm() {
        if (stagiaireSelectionne != null) confirmerSuppression(stagiaireSelectionne);
    }

    private void confirmerSuppression(Stagiaire s) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer « " + s.getNomComplet() + " » ?", ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmation"); a.setHeaderText(null);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (stagiaireService.supprimerStagiaire(s.getId())) {
                    afficherInfo("Suppression", "Stagiaire supprimé.");
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
        stagiaireSelectionne = null;
        if (lblFormTitle != null) lblFormTitle.setText("Détails du Stagiaire");
        setVisibilite(false, false, false);
        viderFormulaire();
        setDisableFormulaire(true);
    }

    @FXML private void handleFiltreChange()   { appliquerFiltres(); }
    @FXML private void handleReinitialiser()  {
        txtRecherche.clear();
        if (cmbSpecialite != null) cmbSpecialite.getSelectionModel().selectFirst();
        if (cmbLangue     != null) cmbLangue.getSelectionModel().selectFirst();
        if (cmbStatut     != null) cmbStatut.getSelectionModel().selectFirst();
        appliquerFiltres();
    }

    @FXML private void handleImporter() { afficherInfo("Import", "Import en développement."); }
    @FXML private void handleExporter() { afficherInfo("Export", "Export en développement."); }

    @FXML private void handlePagePrecedente() {
        if (currentPage > 0) { currentPage--; mettreAJourPagination(); }
    }
    @FXML private void handlePageSuivante() {
        int total = (int) Math.ceil((double) stagiairesFiltered.size() / pageSize);
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
