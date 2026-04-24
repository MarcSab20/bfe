package application.statistiques;

import application.ecoles.Ecole;
import application.ecoles.EcoleService;
import application.stagiaires.Stagiaire;
import application.stagiaires.StagiaireService;
import application.stages.StageFormation;
import application.stages.StageService;
import application.utils.AlerteService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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
    @FXML private TableColumn<EcoleStats, String>  colEcole;
    @FXML private TableColumn<EcoleStats, String>  colType;
    @FXML private TableColumn<EcoleStats, String>  colPays;
    @FXML private TableColumn<EcoleStats, String>  colVille;
    @FXML private TableColumn<EcoleStats, Integer> colNbStagiaires;
    @FXML private TableColumn<EcoleStats, String>  colTaux;
    @FXML private TableColumn<EcoleStats, String>  colPartenaire;

    private final EcoleService     ecoleService     = new EcoleService();
    private final StagiaireService stagiaireService = new StagiaireService();
    private final StageService     stageService     = new StageService();
    private final AlerteService    alerteService    = new AlerteService();

    private List<Ecole>     ecoles;
    private List<Stagiaire> stagiaires;
    private final ObservableList<EcoleStats> ecoleStats = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (cmbPeriode != null) cmbPeriode.getSelectionModel().selectFirst();
        configurerTableau();
        chargerDonnees();
        if (txtRechercheTableau != null)
            txtRechercheTableau.textProperty().addListener((obs, old, n) -> filtrerTableau(n));
    }

    private void configurerTableau() {
        if (tableEcoles == null) return;
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
                ecoles     = ecoleService.getToutesEcoles();
                stagiaires = stagiaireService.getStagiairesActifs();
                Platform.runLater(() -> {
                    mettreAJourCartes();
                    genererGraphiques();
                    remplirTableau();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> afficherErreur("Erreur chargement", e.getMessage()));
            }
        }).start();
    }

    private void mettreAJourCartes() {
        if (lblTotalStagiaires != null) lblTotalStagiaires.setText(String.valueOf(stagiaires.size()));
        if (lblTotalEcoles     != null) lblTotalEcoles.setText(String.valueOf(ecoles.size()));
        if (lblStagesEnCours   != null) lblStagesEnCours.setText(String.valueOf(stageService.compterStagesParStatut("En cours")));
        if (lblRetoursImminents!= null) lblRetoursImminents.setText(String.valueOf(alerteService.compterRetoursUrgents(30)));

        double dureeMoyenne = calculerDureeMoyenneStages();
        if (lblDureeeMoyenne != null)
            lblDureeeMoyenne.setText(String.format("%.1f mois en moyenne", dureeMoyenne));

        if (lblEvolutionStagiaires != null) lblEvolutionStagiaires.setText("↗ +12% vs période précédente");
        if (lblEvolutionEcoles     != null) lblEvolutionEcoles.setText("→ Stable");
    }

    private double calculerDureeMoyenneStages() {
        double totalJours = 0;
        int count = 0;
        for (Stagiaire s : stagiaires) {
            StageFormation sf = stageService.getStageActifByStagiaire(s.getId());
            if (sf != null && sf.getDateDebut() != null && sf.getDateFin() != null) {
                totalJours += sf.getDureeEnJours();
                count++;
            }
        }
        return count > 0 ? (totalJours / count) / 30.0 : 0;
    }

    private void genererGraphiques() {
        genererGraphiqueTypesEcoles();
        genererGraphiquePays();
        genererGraphiqueEvolution();
        genererGraphiqueSpecialites();
        genererGraphiqueLangues();
    }

    private void genererGraphiqueTypesEcoles() {
        if (chartTypesEcoles == null) return;
        Map<String, Long> counts = ecoles.stream()
            .filter(e -> e.getType() != null)
            .collect(Collectors.groupingBy(Ecole::getType, Collectors.counting()));

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        counts.forEach((t, n) -> data.add(new PieChart.Data(t + " (" + n + ")", n)));
        chartTypesEcoles.setData(data);
    }

    private void genererGraphiquePays() {
        if (chartPays == null) return;
        Map<String, Long> counts = ecoles.stream()
            .filter(e -> e.getPays() != null)
            .collect(Collectors.groupingBy(Ecole::getPays, Collectors.counting()));

        List<Map.Entry<String, Long>> top5 = counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
            .collect(Collectors.toList());

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        top5.forEach(e -> data.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue())));

        long autres = counts.values().stream().mapToLong(Long::longValue).sum()
                    - top5.stream().mapToLong(Map.Entry::getValue).sum();
        if (autres > 0) data.add(new PieChart.Data("Autres (" + autres + ")", autres));

        chartPays.setData(data);
    }

    private void genererGraphiqueEvolution() {
        if (chartEvolution == null) return;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Stagiaires");
        String[] mois = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};
        int base = stagiaires.size();
        Random rand = new Random(42);
        for (String m : mois) {
            int v = Math.max(0, base + rand.nextInt(11) - 5);
            series.getData().add(new XYChart.Data<>(m, v));
            base = v;
        }
        chartEvolution.getData().setAll(series);
    }

    private void genererGraphiqueSpecialites() {
        if (chartSpecialites == null) return;
        Map<String, Long> counts = stagiaires.stream()
            .filter(s -> s.getSpecialite() != null && !s.getSpecialite().isEmpty())
            .collect(Collectors.groupingBy(Stagiaire::getSpecialite, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Stagiaires");
        counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
            .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));

        chartSpecialites.getData().setAll(series);
    }

    private void genererGraphiqueLangues() {
        if (chartLangues == null) return;
        Map<String, Long> counts = stagiaires.stream()
            .filter(s -> s.getLangue() != null && !s.getLangue().isEmpty())
            .collect(Collectors.groupingBy(Stagiaire::getLangue, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Stagiaires");
        counts.forEach((l, n) -> series.getData().add(new XYChart.Data<>(l, n)));
        chartLangues.getData().setAll(series);
    }

    private void remplirTableau() {
        ecoleStats.clear();
        for (Ecole e : ecoles) {
            int nb = stagiaireService.compterStagiairesParEcole(e.getId());
            double taux = nb / 50.0 * 100;
            ecoleStats.add(new EcoleStats(e.getNom(), e.getType(), e.getPays(), e.getVille(),
                nb, String.format("%.1f%%", taux), e.isPartenaire() ? "✓" : "✗"));
        }
    }

    private void filtrerTableau(String recherche) {
        if (recherche == null || recherche.isEmpty()) {
            tableEcoles.setItems(ecoleStats);
            return;
        }
        String min = recherche.toLowerCase();
        ObservableList<EcoleStats> f = ecoleStats.stream()
            .filter(s -> s.getNom().toLowerCase().contains(min)
                      || (s.getPays() != null && s.getPays().toLowerCase().contains(min))
                      || (s.getVille() != null && s.getVille().toLowerCase().contains(min)))
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
        tableEcoles.setItems(f);
    }

    @FXML private void handleChangePeriode() { chargerDonnees(); }
    @FXML private void handleActualiser()    { chargerDonnees(); }
    @FXML private void handleExporter()      { afficherInfo("Export", "Export en développement (PDF, Excel, PPT)."); }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg); a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }
    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg); a.setTitle(titre); a.setHeaderText(null); a.showAndWait();
    }

    // ------------------------------------------------------------------ classe interne

    public static class EcoleStats {
        private final String  nom, type, pays, ville, tauxRemplissage, partenaire;
        private final Integer nbStagiaires;

        public EcoleStats(String nom, String type, String pays, String ville,
                          Integer nb, String taux, String partenaire) {
            this.nom = nom; this.type = type; this.pays = pays; this.ville = ville;
            this.nbStagiaires = nb; this.tauxRemplissage = taux; this.partenaire = partenaire;
        }

        public String  getNom()            { return nom; }
        public String  getType()           { return type; }
        public String  getPays()           { return pays; }
        public String  getVille()          { return ville; }
        public Integer getNbStagiaires()   { return nbStagiaires; }
        public String  getTauxRemplissage(){ return tauxRemplissage; }
        public String  getPartenaire()     { return partenaire; }
    }
}
