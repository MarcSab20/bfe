package application.statistiques;

import application.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller pour la vue Statistiques
 */
public class StatistiquesViewController implements Initializable {
    
    @FXML private ComboBox<String> cmbPeriode;
    @FXML private Label lblTotalStagiaires;
    @FXML private Label lblTotalEcoles;
    @FXML private Label lblStagesEnCours;
    @FXML private Label lblRetoursImminents;
    @FXML private Label lblEvolutionStagiaires;
    @FXML private Label lblEvolutionEcoles;
    @FXML private Label lblDureeeMoyenne;
    
    @FXML private PieChart chartTypesEcoles;
    @FXML private PieChart chartPays;
    @FXML private LineChart<String, Number> chartEvolution;
    @FXML private BarChart<String, Number> chartSpecialites;
    @FXML private BarChart<String, Number> chartLangues;
    
    @FXML private TextField txtRechercheTableau;
    @FXML private TableView<EcoleStats> tableEcoles;
    @FXML private TableColumn<EcoleStats, String> colEcole;
    @FXML private TableColumn<EcoleStats, String> colType;
    @FXML private TableColumn<EcoleStats, String> colPays;
    @FXML private TableColumn<EcoleStats, String> colVille;
    @FXML private TableColumn<EcoleStats, Integer> colNbStagiaires;
    @FXML private TableColumn<EcoleStats, String> colTaux;
    @FXML private TableColumn<EcoleStats, String> colPartenaire;
    
    // Services
    private EcoleService ecoleService;
    private StagiaireService stagiaireService;
    private StageService stageService;
    private AlerteService alerteService;
    
    // Données
    private List<Ecole> ecoles;
    private List<Stagiaire> stagiaires;
    private ObservableList<EcoleStats> ecoleStats = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les services
        ecoleService = new EcoleService();
        stagiaireService = new StagiaireService();
        stageService = new StageService();
        alerteService = new AlerteService();
        
        // Sélectionner la première période
        cmbPeriode.getSelectionModel().selectFirst();
        
        // Configurer le tableau
        configurerTableau();
        
        // Charger les données
        chargerDonnees();
        
        // Configurer la recherche dans le tableau
        txtRechercheTableau.textProperty().addListener((obs, old, newVal) -> filtrerTableau(newVal));
    }
    
    private void configurerTableau() {
        colEcole.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPays.setCellValueFactory(new PropertyValueFactory<>("pays"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colNbStagiaires.setCellValueFactory(new PropertyValueFactory<>("nbStagiaires"));
        colTaux.setCellValueFactory(new PropertyValueFactory<>("tauxRemplissage"));
        colPartenaire.setCellValueFactory(new PropertyValueFactory<>("partenaire"));
        
        tableEcoles.setItems(ecoleStats);
    }
    
    private void chargerDonnees() {
        new Thread(() -> {
            try {
                // Charger les données
                ecoles = ecoleService.getToutesEcoles();
                stagiaires = stagiaireService.getStagiairesActifs();
                
                Platform.runLater(() -> {
                    // Mettre à jour les cartes
                    mettreAJourCartes();
                    
                    // Générer les graphiques
                    genererGraphiques();
                    
                    // Remplir le tableau
                    remplirTableau();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> afficherErreur("Erreur", e.getMessage()));
            }
        }).start();
    }
    
    private void mettreAJourCartes() {
        // Total stagiaires
        lblTotalStagiaires.setText(String.valueOf(stagiaires.size()));
        
        // Total écoles
        int ecolesActives = (int) ecoles.stream().filter(Ecole::isActif).count();
        lblTotalEcoles.setText(String.valueOf(ecolesActives));
        
        // Stages en cours
        int stagesEnCours = stagiaires.size(); // Tous les stagiaires actifs ont un stage en cours
        lblStagesEnCours.setText(String.valueOf(stagesEnCours));
        
        // Durée moyenne des stages
        double dureeMoyenne = calculerDureeMoyenneStages();
        lblDureeeMoyenne.setText(String.format("Durée moyenne: %.1f mois", dureeMoyenne));
        
        // Retours imminents
        int retoursImminents = alerteService.compterRetoursUrgents(30);
        lblRetoursImminents.setText(String.valueOf(retoursImminents));
        
        // Évolutions (simulées pour l'exemple)
        lblEvolutionStagiaires.setText("↗ +12% vs période précédente");
        lblEvolutionEcoles.setText("→ +0% vs période précédente");
    }
    
    private double calculerDureeMoyenneStages() {
        double totalJours = 0;
        int count = 0;
        
        for (Stagiaire stagiaire : stagiaires) {
            StageFormation stage = stageService.getStageActifByStagiaire(stagiaire.getId());
            if (stage != null && stage.getDateDebut() != null && stage.getDateFin() != null) {
                long jours = java.time.temporal.ChronoUnit.DAYS.between(
                    stage.getDateDebut(), stage.getDateFin()
                );
                totalJours += jours;
                count++;
            }
        }
        
        return count > 0 ? (totalJours / count) / 30.0 : 0; // Convertir en mois
    }
    
    private void genererGraphiques() {
        genererGraphiqueTypesEcoles();
        genererGraphiquePays();
        genererGraphiqueEvolution();
        genererGraphiqueSpecialites();
        genererGraphiqueLangues();
    }
    
    private void genererGraphiqueTypesEcoles() {
        Map<String, Long> typesCounts = ecoles.stream()
            .filter(e -> e.getType() != null)
            .collect(Collectors.groupingBy(Ecole::getType, Collectors.counting()));
        
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        
        typesCounts.forEach((type, count) -> {
            pieData.add(new PieChart.Data(type + " (" + count + ")", count));
        });
        
        chartTypesEcoles.setData(pieData);
        chartTypesEcoles.setLegendVisible(true);
    }
    
    private void genererGraphiquePays() {
        Map<String, Long> paysCounts = ecoles.stream()
            .filter(e -> e.getPays() != null)
            .collect(Collectors.groupingBy(Ecole::getPays, Collectors.counting()));
        
        // Prendre les 5 premiers pays
        List<Map.Entry<String, Long>> topPays = paysCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());
        
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        
        topPays.forEach(entry -> {
            pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", 
                                         entry.getValue()));
        });
        
        // Ajouter "Autres" si nécessaire
        long autresCount = paysCounts.values().stream().mapToLong(Long::longValue).sum() 
                         - topPays.stream().mapToLong(Map.Entry::getValue).sum();
        if (autresCount > 0) {
            pieData.add(new PieChart.Data("Autres (" + autresCount + ")", autresCount));
        }
        
        chartPays.setData(pieData);
        chartPays.setLegendVisible(true);
    }
    
    private void genererGraphiqueEvolution() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Stagiaires");
        
        // Simuler une évolution sur 12 mois (en réalité, il faudrait des données historiques)
        String[] mois = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin", 
                        "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};
        
        int baseCount = stagiaires.size();
        Random random = new Random(42); // Seed fixe pour cohérence
        
        for (String moisNom : mois) {
            int variation = random.nextInt(11) - 5; // -5 à +5
            int count = Math.max(0, baseCount + variation);
            series.getData().add(new XYChart.Data<>(moisNom, count));
            baseCount = count;
        }
        
        chartEvolution.getData().clear();
        chartEvolution.getData().add(series);
    }
    
    private void genererGraphiqueSpecialites() {
        Map<String, Long> specialitesCounts = stagiaires.stream()
            .filter(s -> s.getSpecialite() != null && !s.getSpecialite().isEmpty())
            .collect(Collectors.groupingBy(Stagiaire::getSpecialite, Collectors.counting()));
        
        // Prendre les 10 premières spécialités
        List<Map.Entry<String, Long>> topSpecialites = specialitesCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de stagiaires");
        
        topSpecialites.forEach(entry -> {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        });
        
        chartSpecialites.getData().clear();
        chartSpecialites.getData().add(series);
    }
    
    private void genererGraphiqueLangues() {
        Map<String, Long> languesCounts = stagiaires.stream()
            .filter(s -> s.getLangue() != null && !s.getLangue().isEmpty())
            .collect(Collectors.groupingBy(Stagiaire::getLangue, Collectors.counting()));
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de stagiaires");
        
        languesCounts.forEach((langue, count) -> {
            series.getData().add(new XYChart.Data<>(langue, count));
        });
        
        chartLangues.getData().clear();
        chartLangues.getData().add(series);
    }
    
    private void remplirTableau() {
        ecoleStats.clear();
        
        for (Ecole ecole : ecoles) {
            int nbStagiaires = stagiaireService.compterStagiairesParEcole(ecole.getId());
            
            // Calculer le taux de remplissage (exemple avec capacité max de 50)
            int capaciteMax = 50;
            double taux = (double) nbStagiaires / capaciteMax * 100;
            String tauxStr = String.format("%.1f%%", taux);
            
            EcoleStats stats = new EcoleStats(
                ecole.getNom(),
                ecole.getType(),
                ecole.getPays(),
                ecole.getVille(),
                nbStagiaires,
                tauxStr,
                ecole.isPartenaire() ? "✓" : "✗"
            );
            
            ecoleStats.add(stats);
        }
    }
    
    private void filtrerTableau(String recherche) {
        if (recherche == null || recherche.isEmpty()) {
            remplirTableau();
            return;
        }
        
        String rechercheMin = recherche.toLowerCase();
        ObservableList<EcoleStats> filtre = ecoleStats.stream()
            .filter(s -> 
                s.getNom().toLowerCase().contains(rechercheMin) ||
                s.getPays().toLowerCase().contains(rechercheMin) ||
                s.getVille().toLowerCase().contains(rechercheMin)
            )
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        tableEcoles.setItems(filtre);
    }
    
    // Gestionnaires d'événements
    @FXML
    private void handleChangePeriode() {
        chargerDonnees();
    }
    
    @FXML
    private void handleActualiser() {
        chargerDonnees();
    }
    
    @FXML
    private void handleExporter() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText("Exportation des statistiques");
        alert.setContentText("Fonctionnalité d'export en développement.\n" +
                           "Formats prévus: PDF, Excel, PowerPoint");
        alert.showAndWait();
    }
    
    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Classe interne pour les statistiques d'école
    public static class EcoleStats {
        private final String nom;
        private final String type;
        private final String pays;
        private final String ville;
        private final Integer nbStagiaires;
        private final String tauxRemplissage;
        private final String partenaire;
        
        public EcoleStats(String nom, String type, String pays, String ville, 
                         Integer nbStagiaires, String tauxRemplissage, String partenaire) {
            this.nom = nom;
            this.type = type;
            this.pays = pays;
            this.ville = ville;
            this.nbStagiaires = nbStagiaires;
            this.tauxRemplissage = tauxRemplissage;
            this.partenaire = partenaire;
        }
        
        public String getNom() { return nom; }
        public String getType() { return type; }
        public String getPays() { return pays; }
        public String getVille() { return ville; }
        public Integer getNbStagiaires() { return nbStagiaires; }
        public String getTauxRemplissage() { return tauxRemplissage; }
        public String getPartenaire() { return partenaire; }
    }
}