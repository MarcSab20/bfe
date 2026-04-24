package application.ecoles;

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
import java.util.*;
import java.util.stream.Collectors;

public class EcolesViewController implements Initializable {

    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbPays;
    @FXML private CheckBox chkPartenaires;
    @FXML private Label lblTotal;

    @FXML private TableView<Ecole> tableEcoles;
    @FXML private TableColumn<Ecole, String> colNom;
    @FXML private TableColumn<Ecole, String> colType;
    @FXML private TableColumn<Ecole, String> colPays;
    @FXML private TableColumn<Ecole, String> colVille;
    @FXML private TableColumn<Ecole, Integer> colStagiaires;
    @FXML private TableColumn<Ecole, String> colPartenaire;
    @FXML private TableColumn<Ecole, Void> colActions;

    @FXML private Label lblPage;
    @FXML private ComboBox<String> cmbPageSize;

    @FXML private Label lblFormTitle;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnSupprimer;
    @FXML private TextField txtNom;
    @FXML private ComboBox<String> cmbFormType;
    @FXML private CheckBox chkFormPartenaire;
    @FXML private ComboBox<String> cmbFormPays;
    @FXML private TextField txtVille;
    @FXML private TextArea txtAdresse;
    @FXML private TextField txtLatitude;
    @FXML private TextField txtLongitude;
    @FXML private TextField txtContact;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtSiteWeb;
    @FXML private TextArea txtSpecialites;
    @FXML private TextArea txtDescription;

    private final EcoleService ecoleService = new EcoleService();
    private final application.stagiaires.StagiaireService stagiaireService = new application.stagiaires.StagiaireService();

    private final ObservableList<Ecole> ecolesData     = FXCollections.observableArrayList();
    private final ObservableList<Ecole> ecolesFiltered = FXCollections.observableArrayList();

    private Ecole ecoleSelectionnee;
    private boolean modeEdition = false;
    private int currentPage = 0;
    private int pageSize = 25;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurerTableau();
        chargerComboTypes();
        chargerPaysDansForm();

        cmbType.getSelectionModel().selectFirst();
        if (cmbPageSize != null) cmbPageSize.getSelectionModel().select("25");

        chargerDonnees();

        txtRecherche.textProperty().addListener((obs, old, n) -> appliquerFiltres());
        tableEcoles.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> {
            if (n != null && !modeEdition) afficherDetailsEcole(n);
        });
    }

    private void chargerComboTypes() {
        cmbType.getItems().setAll("Tous les types", "Professionnelle", "Académique", "Mixte", "Autre");
    }

    private void chargerPaysDansForm() {
        List<String> pays = Arrays.asList(
            "Cameroun", "France", "Allemagne", "États-Unis", "Canada",
            "Royaume-Uni", "Belgique", "Suisse", "Sénégal", "Côte d'Ivoire",
            "Gabon", "Congo", "RDC", "Tchad", "Guinée Équatoriale",
            "Maroc", "Tunisie", "Algérie", "Égypte", "Afrique du Sud"
        );
        cmbFormPays.getItems().addAll(pays);

        // types dans le formulaire
        cmbFormType.getItems().setAll("Professionnelle", "Académique", "Mixte", "Autre");
    }

    private void configurerTableau() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPays.setCellValueFactory(new PropertyValueFactory<>("pays"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));

        colStagiaires.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleObjectProperty<>(
                stagiaireService.compterStagiairesParEcole(cell.getValue().getId())));

        colPartenaire.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().isPartenaire() ? "✓" : "✗"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnMod = new Button("✏");
            private final Button btnDel = new Button("✕");
            private final HBox box = new HBox(5, btnMod, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnMod.getStyleClass().add("btn-action-small");
                btnDel.getStyleClass().add("btn-danger-small");
                btnMod.setOnAction(e -> modifierEcole(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableEcoles.setItems(ecolesFiltered);
    }

    private void chargerDonnees() {
        new Thread(() -> {
            try {
                List<Ecole> ecoles = ecoleService.getToutesEcoles();
                Platform.runLater(() -> {
                    ecolesData.setAll(ecoles);
                    chargerFiltrePays();
                    appliquerFiltres();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> afficherErreur("Erreur", e.getMessage()));
            }
        }).start();
    }

    private void chargerFiltrePays() {
        String selPays = cmbPays != null ? cmbPays.getValue() : null;
        List<String> pays = ecolesData.stream()
            .map(Ecole::getPays).filter(Objects::nonNull)
            .distinct().sorted().collect(Collectors.toList());
        if (cmbPays != null) {
            cmbPays.getItems().setAll("Tous les pays");
            cmbPays.getItems().addAll(pays);
            cmbPays.setValue(selPays != null ? selPays : "Tous les pays");
        }
    }

    private void appliquerFiltres() {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String type  = cmbType != null ? cmbType.getValue() : null;
        String pays  = cmbPays != null ? cmbPays.getValue()  : null;
        boolean partenairesOnly = chkPartenaires != null && chkPartenaires.isSelected();

        List<Ecole> filtered = ecolesData.stream().filter(e -> {
            if (!recherche.isEmpty()) {
                String nom   = e.getNom()   != null ? e.getNom().toLowerCase()   : "";
                String paysE = e.getPays()  != null ? e.getPays().toLowerCase()  : "";
                String ville = e.getVille() != null ? e.getVille().toLowerCase() : "";
                if (!nom.contains(recherche) && !paysE.contains(recherche) && !ville.contains(recherche))
                    return false;
            }
            if (type != null && !type.equals("Tous les types") && !type.equals(e.getType())) return false;
            if (pays != null && !pays.equals("Tous les pays") && !pays.equals(e.getPays()))  return false;
            if (partenairesOnly && !e.isPartenaire()) return false;
            return true;
        }).collect(Collectors.toList());

        ecolesFiltered.setAll(filtered);
        if (lblTotal != null) lblTotal.setText(filtered.size() + " école(s)");
        currentPage = 0;
        mettreAJourPagination();
    }

    private void mettreAJourPagination() {
        if (lblPage == null) return;
        int total = (int) Math.ceil((double) ecolesFiltered.size() / pageSize);
        lblPage.setText("Page " + (currentPage + 1) + " / " + Math.max(1, total));
    }

    private void afficherDetailsEcole(Ecole ecole) {
        ecoleSelectionnee = ecole;
        if (lblFormTitle != null) lblFormTitle.setText("Détails de l'École");
        setVisibiliteFormulaire(false, false, false);
        remplirFormulaire(ecole);
        desactiverFormulaire();
    }

    private void modifierEcole(Ecole ecole) {
        ecoleSelectionnee = ecole;
        modeEdition = true;
        if (lblFormTitle != null) lblFormTitle.setText("Modifier l'École");
        setVisibiliteFormulaire(true, true, true);
        remplirFormulaire(ecole);
        activerFormulaire();
    }

    private void setVisibiliteFormulaire(boolean annuler, boolean enregistrer, boolean supprimer) {
        if (btnAnnuler   != null) btnAnnuler.setVisible(annuler);
        if (btnEnregistrer != null) btnEnregistrer.setVisible(enregistrer);
        if (btnSupprimer != null) btnSupprimer.setVisible(supprimer);
    }

    private void remplirFormulaire(Ecole e) {
        txtNom.setText(nvl(e.getNom()));
        cmbFormType.setValue(e.getType());
        chkFormPartenaire.setSelected(e.isPartenaire());
        cmbFormPays.setValue(e.getPays());
        txtVille.setText(nvl(e.getVille()));
        if (txtAdresse != null) txtAdresse.setText(nvl(e.getAdresse()));
        txtLatitude.setText(e.getLatitude() != null ? e.getLatitude().toString() : "");
        txtLongitude.setText(e.getLongitude() != null ? e.getLongitude().toString() : "");
        txtContact.setText(nvl(e.getContact()));
        txtEmail.setText(nvl(e.getEmail()));
        txtTelephone.setText(nvl(e.getTelephone()));
        txtSiteWeb.setText(nvl(e.getSiteWeb()));
        if (txtSpecialites != null)
            txtSpecialites.setText(e.getSpecialites() != null ? String.join(", ", e.getSpecialites()) : "");
        if (txtDescription != null) txtDescription.setText(nvl(e.getDescription()));
    }

    private void viderFormulaire() {
        txtNom.clear(); cmbFormType.getSelectionModel().clearSelection();
        chkFormPartenaire.setSelected(false); cmbFormPays.getSelectionModel().clearSelection();
        txtVille.clear(); txtLatitude.clear(); txtLongitude.clear();
        txtContact.clear(); txtEmail.clear(); txtTelephone.clear(); txtSiteWeb.clear();
        if (txtAdresse != null) txtAdresse.clear();
        if (txtSpecialites != null) txtSpecialites.clear();
        if (txtDescription != null) txtDescription.clear();
    }

    private void activerFormulaire() {
        setDisableFormulaire(false);
    }

    private void desactiverFormulaire() {
        setDisableFormulaire(true);
    }

    private void setDisableFormulaire(boolean disabled) {
        txtNom.setDisable(disabled); cmbFormType.setDisable(disabled);
        chkFormPartenaire.setDisable(disabled); cmbFormPays.setDisable(disabled);
        txtVille.setDisable(disabled); txtLatitude.setDisable(disabled);
        txtLongitude.setDisable(disabled); txtContact.setDisable(disabled);
        txtEmail.setDisable(disabled); txtTelephone.setDisable(disabled);
        txtSiteWeb.setDisable(disabled);
        if (txtAdresse != null) txtAdresse.setDisable(disabled);
        if (txtSpecialites != null) txtSpecialites.setDisable(disabled);
        if (txtDescription != null) txtDescription.setDisable(disabled);
    }

    // ------------------------------------------------------------------ actions FXML

    @FXML
    private void handleNouvelleEcole() {
        ecoleSelectionnee = null;
        modeEdition = true;
        if (lblFormTitle != null) lblFormTitle.setText("Nouvelle École");
        setVisibiliteFormulaire(true, true, false);
        viderFormulaire();
        activerFormulaire();
    }

    @FXML
    private void handleEnregistrer() {
        if (!valider()) return;

        Ecole ecole = ecoleSelectionnee != null ? ecoleSelectionnee : new Ecole();
        lireFormulaire(ecole);

        boolean ok;
        if (ecoleSelectionnee != null && ecoleSelectionnee.getId() != null) {
            ok = ecoleService.mettreAJourEcole(ecole);
        } else {
            ecole.setActif(true);
            ok = ecoleService.creerEcole(ecole);
        }

        if (ok) {
            afficherInfo("Succès", "École enregistrée avec succès.");
            chargerDonnees();
            handleAnnuler();
        } else {
            afficherErreur("Erreur", "Échec de l'enregistrement.");
        }
    }

    private void lireFormulaire(Ecole e) {
        e.setNom(txtNom.getText().trim());
        e.setType(cmbFormType.getValue());
        e.setPartenaire(chkFormPartenaire.isSelected());
        e.setPays(cmbFormPays.getValue());
        e.setVille(txtVille.getText().trim());
        if (txtAdresse != null) e.setAdresse(txtAdresse.getText().trim());
        try {
            if (!txtLatitude.getText().isEmpty())
                e.setLatitude(Double.parseDouble(txtLatitude.getText()));
            if (!txtLongitude.getText().isEmpty())
                e.setLongitude(Double.parseDouble(txtLongitude.getText()));
        } catch (NumberFormatException ex) {
            afficherErreur("Erreur", "Latitude et longitude doivent être des nombres.");
        }
        e.setContact(txtContact.getText().trim());
        e.setEmail(txtEmail.getText().trim());
        e.setTelephone(txtTelephone.getText().trim());
        e.setSiteWeb(txtSiteWeb.getText().trim());
        if (txtSpecialites != null && !txtSpecialites.getText().isEmpty()) {
            List<String> sp = Arrays.stream(txtSpecialites.getText().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            e.setSpecialites(sp);
        }
        if (txtDescription != null) e.setDescription(txtDescription.getText().trim());
    }

    private boolean valider() {
        if (txtNom.getText().trim().isEmpty()) {
            afficherErreur("Validation", "Le nom est requis."); return false;
        }
        if (cmbFormType.getValue() == null) {
            afficherErreur("Validation", "Le type est requis."); return false;
        }
        if (cmbFormPays.getValue() == null) {
            afficherErreur("Validation", "Le pays est requis."); return false;
        }
        if (txtVille.getText().trim().isEmpty()) {
            afficherErreur("Validation", "La ville est requise."); return false;
        }
        return true;
    }

    @FXML
    private void handleSupprimer() {
        if (ecoleSelectionnee != null) confirmerSuppression(ecoleSelectionnee);
    }

    private void confirmerSuppression(Ecole ecole) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer « " + ecole.getNom() + " » ?", ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmation");
        a.setHeaderText(null);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (ecoleService.supprimerEcole(ecole.getId())) {
                    afficherInfo("Suppression", "École supprimée.");
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
        ecoleSelectionnee = null;
        if (lblFormTitle != null) lblFormTitle.setText("Détails de l'École");
        setVisibiliteFormulaire(false, false, false);
        viderFormulaire();
        desactiverFormulaire();
    }

    @FXML private void handleReinitialiserForm() {
        if (ecoleSelectionnee != null) remplirFormulaire(ecoleSelectionnee);
        else viderFormulaire();
    }

    @FXML private void handleFiltreChange()   { appliquerFiltres(); }
    @FXML private void handleReinitialiser()  {
        txtRecherche.clear();
        cmbType.getSelectionModel().selectFirst();
        if (cmbPays != null) cmbPays.getSelectionModel().selectFirst();
        if (chkPartenaires != null) chkPartenaires.setSelected(false);
        appliquerFiltres();
    }

    @FXML private void handleImporter() { afficherInfo("Import", "Fonctionnalité d'import en développement."); }
    @FXML private void handleExporter() { afficherInfo("Export", "Fonctionnalité d'export en développement."); }

    @FXML private void handlePagePrecedente() {
        if (currentPage > 0) { currentPage--; mettreAJourPagination(); }
    }
    @FXML private void handlePageSuivante() {
        int total = (int) Math.ceil((double) ecolesFiltered.size() / pageSize);
        if (currentPage < total - 1) { currentPage++; mettreAJourPagination(); }
    }
    @FXML private void handlePageSizeChange() {
        if (cmbPageSize != null && cmbPageSize.getValue() != null) {
            try { pageSize = Integer.parseInt(cmbPageSize.getValue()); } catch (NumberFormatException ignored) {}
            currentPage = 0; mettreAJourPagination();
        }
    }

    @FXML private void handleLocaliserCarte() {
        afficherInfo("Carte", "Localisation sur carte en développement.");
    }

    // ------------------------------------------------------------------ utilitaires

    private String nvl(String s) { return s != null ? s : ""; }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }

    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }
}
