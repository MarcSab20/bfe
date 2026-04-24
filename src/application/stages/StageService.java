package application.stages;

import application.utils.DatabaseManager;
import application.ecoles.Ecole;
import application.ecoles.EcoleService;
import application.stagiaires.Stagiaire;
import application.stagiaires.StagiaireService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StageService {

    private final EcoleService ecoleService = new EcoleService();
    private final StagiaireService stagiaireService = new StagiaireService();

    // ------------------------------------------------------------------ lecture

    public List<StageFormation> getTousLesStages() {
        List<StageFormation> list = new ArrayList<>();
        String sql = "SELECT * FROM stages ORDER BY date_creation DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<StageFormation> getStagesEnCours() {
        List<StageFormation> list = new ArrayList<>();
        String sql = "SELECT * FROM stages WHERE statut = 'En cours' ORDER BY date_debut DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<StageFormation> getStagesTermines() {
        List<StageFormation> list = new ArrayList<>();
        String sql = "SELECT * FROM stages WHERE statut = 'Terminé' ORDER BY date_fin DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public StageFormation getStageActifByStagiaire(Long stagiaireId) {
        String sql = "SELECT * FROM stages WHERE stagiaire_id = ? AND statut = 'En cours' LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, stagiaireId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public StageFormation getStageParId(Long id) {
        String sql = "SELECT * FROM stages WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<StageFormation> getStagesParEcole(Long ecoleId) {
        List<StageFormation> list = new ArrayList<>();
        String sql = "SELECT * FROM stages WHERE ecole_id = ? ORDER BY date_debut DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ecoleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<StageFormation> getStagesParStagiaire(Long stagiaireId) {
        List<StageFormation> list = new ArrayList<>();
        String sql = "SELECT * FROM stages WHERE stagiaire_id = ? ORDER BY date_debut DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, stagiaireId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int compterStagesParStatut(String statut) {
        String sql = "SELECT COUNT(*) FROM stages WHERE statut = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<StageFormation> getStagesProchainsFin(int joursLimite) {
        List<StageFormation> list = new ArrayList<>();
        String sql = "SELECT * FROM stages WHERE statut = 'En cours' " +
                     "AND DATEDIFF(date_fin, CURDATE()) <= ? AND date_fin >= CURDATE() " +
                     "ORDER BY date_fin ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, joursLimite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ------------------------------------------------------------------ écriture

    public boolean creerStage(StageFormation sf) {
        String sql = "INSERT INTO stages (stagiaire_id,ecole_id,type,date_debut,date_fin," +
                     "specialite,encadrant,tuteur,objectifs,description,statut,document_id," +
                     "document_nom,remarques,date_creation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, sf);
            if (ps.executeUpdate() > 0) {
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) sf.setId(gk.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean mettreAJourStage(StageFormation sf) {
        String sql = "UPDATE stages SET stagiaire_id=?,ecole_id=?,type=?,date_debut=?,date_fin=?," +
                     "specialite=?,encadrant=?,tuteur=?,objectifs=?,description=?,statut=?," +
                     "document_id=?,document_nom=?,remarques=?,date_creation=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, sf);
            ps.setLong(16, sf.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean mettreAJourStatut(Long stageId, String nouveauStatut) {
        String sql = "UPDATE stages SET statut = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setLong(2, stageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean mettreAJourDocument(Long stageId, String documentId, String documentNom) {
        String sql = "UPDATE stages SET document_id = ?, document_nom = ? WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, documentId);
            ps.setString(2, documentNom);
            ps.setLong(3, stageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean supprimerStage(Long id) {
        String sql = "DELETE FROM stages WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private void remplir(PreparedStatement ps, StageFormation sf) throws SQLException {
        ps.setLong(1, sf.getStagiaire() != null ? sf.getStagiaire().getId() : 0);
        ps.setLong(2, sf.getEcole() != null ? sf.getEcole().getId() : 0);
        ps.setString(3, sf.getType());
        ps.setObject(4, sf.getDateDebut());
        ps.setObject(5, sf.getDateFin());
        ps.setString(6, sf.getSpecialite());
        ps.setString(7, sf.getEncadrant());
        ps.setString(8, sf.getTuteur());
        ps.setString(9, sf.getObjectifs());
        ps.setString(10, sf.getDescription());
        ps.setString(11, sf.getStatut());
        ps.setString(12, sf.getDocumentId());
        ps.setString(13, sf.getDocumentNom());
        ps.setString(14, sf.getRemarques());
        ps.setObject(15, sf.getDateCreation());
    }

    private StageFormation mapper(ResultSet rs) throws SQLException {
        StageFormation sf = new StageFormation();
        sf.setId(rs.getLong("id"));
        long stagiaireId = rs.getLong("stagiaire_id");
        long ecoleId = rs.getLong("ecole_id");
        Stagiaire stagiaire = stagiaireService.getStagiaireParId(stagiaireId);
        Ecole ecole = ecoleService.getEcoleParId(ecoleId);
        sf.setStagiaire(stagiaire);
        sf.setEcole(ecole);
        sf.setType(rs.getString("type"));
        sf.setDateDebut(rs.getObject("date_debut", java.time.LocalDate.class));
        sf.setDateFin(rs.getObject("date_fin", java.time.LocalDate.class));
        sf.setSpecialite(rs.getString("specialite"));
        sf.setEncadrant(rs.getString("encadrant"));
        sf.setTuteur(rs.getString("tuteur"));
        sf.setObjectifs(rs.getString("objectifs"));
        sf.setDescription(rs.getString("description"));
        sf.setStatut(rs.getString("statut"));
        sf.setDocumentId(rs.getString("document_id"));
        sf.setDocumentNom(rs.getString("document_nom"));
        sf.setRemarques(rs.getString("remarques"));
        sf.setDateCreation(rs.getObject("date_creation", java.time.LocalDate.class));
        return sf;
    }
}
