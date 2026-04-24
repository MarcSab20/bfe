package application.views.ecoles;

import application.*;
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

/**
 * Controller pour la gestion des écoles
 */
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
    @FXML private Button btnEnregistrer;
    @FXML private Button btnSupprimer;
    
    // Services
    private EcoleService ecoleService;
    private StagiaireService stagiaireService;
    
    // Données
    private ObservableList<Ecole> ecolesData = FXCollections.observableArrayList();
    private ObservableList<Ecole> ecolesFiltered = FXCollections.observableArrayList();
    private Ecole ecoleSelectionnee;
    private boolean modeEdition = false;
    
    // Pagination
    private int currentPage = 0;
    private int pageSize = 25;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les services
        ecoleService = new EcoleService();
        stagiaireService = new StagiaireService();
        
        // Configurer le tableau
        configurerTableau();
        
        // Charger les filtres
        chargerFiltres();
        
        // Sélectionner les valeurs par défaut
        cmbType.getSelectionModel().selectFirst();
        cmbPageSize.getSelectionModel().select("25");
        
        // Charger les données
        chargerDonnees();
        
        // Configurer la recherche en temps réel
        txtRecherche.textProperty().addListener((obs, old, newVal) -> appliquerFiltres());
        
        // Listener de sélection dans le tableau
        tableEcoles.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !modeEdition) {
                afficherDetailsEcole(newVal);
            }
        });
        
        // Charger les pays dans le formulaire
        chargerPaysDansForm();
    }
    
    private void configurerTableau() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPays.setCellValueFactory(new PropertyValueFactory<>("pays"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        
        // Colonne personnalisée pour le nombre de stagiaires
        colStagiaires.setCellValueFactory(cellData -> {
            int nb = stagiaireService.compterStagiairesParEcole(cellData.getValue().getId());
            return new javafx.beans.property.SimpleObjectProperty<>(nb);
        });
        
        // Colonne personnalisée pour le partenaire
        colPartenaire.setCellValueFactory(cellData -> {
            String val = cellData.getValue().isPartenaire() ? "✓" : "✗";
            return new javafx.beans.property.SimpleStringProperty(val);
        });
        
        // Colonne d'actions
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("✏️");
            private final Button btnSupprimer = new Button("🗑️");
            private final HBox hbox = new HBox(5, btnModifier, btnSupprimer);
            
            {
                hbox.setAlignment(Pos.CENTER);
                btnModifier.getStyleClass().add("btn-action-small");
                btnSupprimer.getStyleClass().add("btn-danger-small");
                
                btnModifier.setOnAction(event -> {
                    Ecole ecole = getTableView().getItems().get(getIndex());
                    modifierEcole(ecole);
                });
                
                btnSupprimer.setOnAction(event -> {
                    Ecole ecole = getTableView().getItems().get(getIndex());
                    confirmerSuppression(ecole);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        
        tableEcoles.setItems(ecolesFiltered);
    }
    
    private void chargerFiltres() {
        try {
            // Charger les pays
            List<String> pays = ecolesData.stream()
                .map(Ecole::getPays)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            cmbPays.getItems().clear();
            cmbPays.getItems().add("Tous les pays");
            cmbPays.getItems().addAll(pays);
            cmbPays.getSelectionModel().selectFirst();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void chargerPaysDansForm() {
        List<String> pays = Arrays.asList(
            "Cameroun", "France", "Allemagne", "États-Unis", "Canada",
            "Royaume-Uni", "Belgique", "Suisse", "Sénégal", "Côte d'Ivoire",
            "Gabon", "Congo", "RDC", "Tchad", "Guinée Équatoriale"
        );
        cmbFormPays.getItems().addAll(pays);
    }
    
    private void chargerDonnees() {
        new Thread(() -> {
            try {
                List<Ecole> ecoles = ecoleService.getToutesEcoles();
                
                Platform.runLater(() -> {
                    ecolesData.clear();
                    ecolesData.addAll(ecoles);
                    appliquerFiltres();
                    chargerFiltres();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> afficherErreur("Erreur", e.getMessage()));
            }
        }).start();
    }
    
    private void appliquerFiltres() {
        String recherche = txtRecherche.getText().toLowerCase();
        String type = cmbType.getValue();
        String pays = cmbPays.getValue();
        boolean partenairesOnly = chkPartenaires.isSelected();
        
        ecolesFiltered.clear();
        
        List<Ecole> filtered = ecolesData.stream()
            .filter(e -> {
                // Filtre recherche
                if (!recherche.isEmpty()) {
                    String nom = e.getNom() != null ? e.getNom().toLowerCase() : "";
                    String paysE = e.getPays() != null ? e.getPays().toLowerCase() : "";
                    String ville = e.getVille() != null ? e.getVille().toLowerCase() : "";
                    
                    if (!nom.contains(recherche) && !paysE.contains(recherche) && 
                        !ville.contains(recherche)) {
                        return false;
                    }
                }
                
                // Filtre type
                if (type != null && !type.equals("Tous les types")) {
                    if (!type.equals(e.getType())) {
                        return false;
                    }
                }
                
                // Filtre pays
                if (pays != null && !pays.equals("Tous les pays")) {
                    if (!pays.equals(e.getPays())) {
                        return false;
                    }
                }
                
                // Filtre partenaires
                if (partenairesOnly && !e.isPartenaire()) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        ecolesFiltered.addAll(filtered);
        lblTotal.setText(filtered.size() + " école(s)");
        
        // Réinitialiser la pagination
        currentPage = 0;
        mettreAJourPagination();
    }
    
    private void mettreAJourPagination() {
        int totalPages = (int) Math.ceil((double) ecolesFiltered.size() / pageSize);
        lblPage.setText("Page " + (currentPage + 1) + " / " + Math.max(1, totalPages));
    }
    
    private void afficherDetailsEcole(Ecole ecole) {
        ecoleSelectionnee = ecole;
        lblFormTitle.setText("📋 Détails de l'École");
        btnAnnuler.setVisible(false);
        btnEnregistrer.setVisible(false);
        btnSupprimer.setVisible(false);
        
        // Remplir le formulaire en lecture seule
        remplirFormulaire(ecole);
        desactiverFormulaire();
    }
    
    private void modifierEcole(Ecole ecole) {
        ecoleSelectionnee = ecole;
        modeEdition = true;
        lblFormTitle.setText("✏️ Modifier l'École");
        btnAnnuler.setVisible(true);
        btnEnregistrer.setVisible(true);
        btnSupprimer.setVisible(true);
        
        remplirFormulaire(ecole);
        activerFormulaire();
    }
    
    private void remplirFormulaire(Ecole ecole) {
        txtNom.setText(ecole.getNom());
        cmbFormType.setValue(ecole.getType());
        chkFormPartenaire.setSelected(ecole.isPartenaire());
        cmbFormPays.setValue(ecole.getPays());
        txtVille.setText(ecole.getVille());
        txtAdresse.setText(ecole.getAdresse());
        txtLatitude.setText(ecole.getLatitude() != null ? ecole.getLatitude().toString() : "");
        txtLongitude.setText(ecole.getLongitude() != null ? ecole.getLongitude().toString() : "");
        txtContact.setText(ecole.getContact());
        txtEmail.setText(ecole.getEmail());
        txtTelephone.setText(ecole.getTelephone());
        txtSiteWeb.setText(ecole.getSiteWeb());
        
        if (ecole.getSpecialites() != null) {
            txtSpecialites.setText(String.join(", ", ecole.getSpecialites()));
        }
        
        txtDescription.setText(ecole.getDescription());
    }
    
    private void viderFormulaire() {
        txtNom.clear();
        cmbFormType.getSelectionModel().clearSelection();
        chkFormPartenaire.setSelected(false);
        cmbFormPays.getSelectionModel().clearSelection();
        txtVille.clear();
        txtAdresse.clear();
        txtLatitude.clear();
        txtLongitude.clear();
        txtContact.clear();
        txtEmail.clear();
        txtTelephone.clear();
        txtSiteWeb.clear();
        txtSpecialites.clear();
        txtDescription.clear();
    }
    
    private void activerFormulaire() {
        txtNom.setDisable(false);
        cmbFormType.setDisable(false);
        chkFormPartenaire.setDisable(false);
        cmbFormPays.setDisable(false);
        txtVille.setDisable(false);
        txtAdresse.setDisable(false);
        txtLatitude.setDisable(false);
        txtLongitude.setDisable(false);
        txtContact.setDisable(false);
        txtEmail.setDisable(false);
        txtTelephone.setDisable(false);
        txtSiteWeb.setDisable(false);
        txtSpecialites.setDisable(false);
        txtDescription.setDisable(false);
    }
    
    private void desactiverFormulaire() {
        txtNom.setDisable(true);
        cmbFormType.setDisable(true);
        chkFormPartenaire.setDisable(true);
        cmbFormPays.setDisable(true);
        txtVille.setDisable(true);
        txtAdresse.setDisable(true);
        txtLatitude.setDisable(true);
        txtLongitude.setDisable(true);
        txtContact.setDisable(true);
        txtEmail.setDisable(true);
        txtTelephone.setDisable(true);
        txtSiteWeb.setDisable(true);
        txtSpecialites.setDisable(true);
        txtDescription.setDisable(true);
    }
    
    // Gestionnaires d'événements
    @FXML
    private void handleNouvelleEcole() {
        ecoleSelectionnee = null;
        modeEdition = true;
        lblFormTitle.setText("➕ Nouvelle École");
        btnAnnuler.setVisible(true);
        btnEnregistrer.setVisible(true);
        btnSupprimer.setVisible(false);
        
        viderFormulaire();
        activerFormulaire();
    }
    
    @FXML
    private void handleEnregistrer() {
        // Validation
        if (!validerFormulaire()) {
            return;
        }
        
        // Créer ou mettre à jour l'école
        Ecole ecole = ecoleSelectionnee != null ? ecoleSelectionnee : new Ecole();
        
        ecole.setNom(txtNom.getText());
        ecole.setType(cmbFormType.getValue());
        ecole.setPartenaire(chkFormPartenaire.isSelected());
        ecole.setPays(cmbFormPays.getValue());
        ecole.setVille(txtVille.getText());
        ecole.setAdresse(txtAdresse.getText());
        
        try {
            if (!txtLatitude.getText().isEmpty()) {
                ecole.setLatitude(Double.parseDouble(txtLatitude.getText()));
            }
            if (!txtLongitude.getText().isEmpty()) {
                ecole.setLongitude(Double.parseDouble(txtLongitude.getText()));
            }
        } catch (NumberFormatException e) {
            afficherErreur("Erreur", "Latitude et longitude doivent être des nombres.");
            return;
        }
        
        ecole.setContact(txtContact.getText());
        ecole.setEmail(txtEmail.getText());
        ecole.setTelephone(txtTelephone.getText());
        ecole.setSiteWeb(txtSiteWeb.getText());
        
        if (!txtSpecialites.getText().isEmpty()) {
            List<String> specialites = Arrays.asList(txtSpecialites.getText().split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
            ecole.setSpecialites(specialites);
        }
        
        ecole.setDescription(txtDescription.getText());
        ecole.setActif(true);
        
        // Enregistrer dans la base de données
        boolean success = ecoleService.creerEcole(ecole);
        
        if (success) {
            afficherInfo("Succès", "École enregistrée avec succès.");
            chargerDonnees();
            handleAnnuler();
        } else {
            afficherErreur("Erreur", "Échec de l'enregistrement de l'école.");
        }
    }
    
    private boolean validerFormulaire() {
        if (txtNom.getText().isEmpty()) {
            afficherErreur("Validation", "Le nom de l'école est requis.");
            return false;
        }
        
        if (cmbFormType.getValue() == null) {
            afficherErreur("Validation", "Le type est requis.");
            return false;
        }
        
        if (cmbFormPays.getValue() == null || cmbFormPays.getValue().isEmpty()) {
            afficherErreur("Validation", "Le pays est requis.");
            return false;
        }
        
        if (txtVille.getText().isEmpty()) {
            afficherErreur("Validation", "La ville est requise.");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void handleSupprimer() {
        if (ecoleSelectionnee != null) {
            confirmerSuppression(ecoleSelectionnee);
        }
    }
    
    private void confirmerSuppression(Ecole ecole) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'école");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer " + ecole.getNom() + " ?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implémenter la suppression
                afficherInfo("Suppression", "École supprimée avec succès.");
                chargerDonnees();
                handleAnnuler();
            }
        });
    }
    
    @FXML
    private void handleAnnuler() {
        modeEdition = false;
        ecoleSelectionnee = null;
        lblFormTitle.setText("📋 Détails de l'École");
        btnAnnuler.setVisible(false);
        btnEnregistrer.setVisible(false);
        btnSupprimer.setVisible(false);
        viderFormulaire();
        desactiverFormulaire();
    }
    
    @FXML
    private void handleReinitialiserForm() {
        if (ecoleSelectionnee != null) {
            remplirFormulaire(ecoleSelectionnee);
        } else {
            viderFormulaire();
        }
    }
    
    @FXML
    private void handleFiltreChange() {
        appliquerFiltres();
    }
    
    @FXML
    private void handleReinitialiser() {
        txtRecherche.clear();
        cmbType.getSelectionModel().selectFirst();
        cmbPays.getSelectionModel().selectFirst();
        chkPartenaires.setSelected(false);
        appliquerFiltres();
    }
    
    @FXML
    private void handleImporter() {
        afficherInfo("Import", "Fonctionnalité d'import en développement.");
    }
    
    @FXML
    private void handleExporter() {
        afficherInfo("Export", "Fonctionnalité d'export en développement.");
    }
    
    @FXML
    private void handlePagePrecedente() {
        if (currentPage > 0) {
            currentPage--;
            mettreAJourPagination();
        }
    }
    
    @FXML
    private void handlePageSuivante() {
        int totalPages = (int) Math.ceil((double) ecolesFiltered.size() / pageSize);
        if (currentPage < totalPages - 1) {
            currentPage++;
            mettreAJourPagination();
        }
    }
    
    @FXML
    private void handlePageSizeChange() {
        String size = cmbPageSize.getValue();
        if (size != null) {
            pageSize = Integer.parseInt(size);
            currentPage = 0;
            mettreAJourPagination();
        }
    }
    
    @FXML
    private void handleLocaliserCarte() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Localisation");
        alert.setHeaderText("Sélection sur carte");
        alert.setContentText("Fonctionnalité de localisation sur carte en développement.");
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