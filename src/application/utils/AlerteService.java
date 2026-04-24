package application.utils;

import application.utils.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AlerteService {

    // ------------------------------------------------------------------ lecture

    public List<Alerte> getAlertesActives() {
        List<Alerte> list = new ArrayList<>();
        String sql = "SELECT a.id, a.stage_id, a.stagiaire_id, a.lue, a.date_creation, " +
                     "CONCAT(s.nom,' ',s.prenom) AS nom_stagiaire, st.type, st.date_fin, " +
                     "e.nom AS nom_ecole, e.pays " +
                     "FROM alertes a " +
                     "JOIN stages st ON a.stage_id = st.id " +
                     "JOIN stagiaires s ON st.stagiaire_id = s.id " +
                     "JOIN ecoles e ON st.ecole_id = e.id " +
                     "WHERE a.lue = false AND st.date_fin >= CURDATE() " +
                     "ORDER BY st.date_fin ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Alerte> getAlertesUrgentes(int joursLimite) {
        List<Alerte> list = new ArrayList<>();
        String sql = "SELECT st.id AS stage_id, st.stagiaire_id, " +
                     "CONCAT(s.nom,' ',s.prenom) AS nom_stagiaire, st.type, st.date_fin, " +
                     "e.nom AS nom_ecole, e.pays " +
                     "FROM stages st " +
                     "JOIN stagiaires s ON st.stagiaire_id = s.id " +
                     "JOIN ecoles e ON st.ecole_id = e.id " +
                     "WHERE st.statut = 'En cours' " +
                     "AND DATEDIFF(st.date_fin, CURDATE()) <= ? " +
                     "AND st.date_fin >= CURDATE() ORDER BY st.date_fin ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, joursLimite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapperUrgent(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int compterRetoursUrgents(int joursLimite) {
        String sql = "SELECT COUNT(*) FROM stages WHERE statut = 'En cours' " +
                     "AND DATEDIFF(date_fin, CURDATE()) <= ? AND date_fin >= CURDATE()";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, joursLimite);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ------------------------------------------------------------------ écriture

    public boolean creerAlerte(Long stageId, Long stagiaireId) {
        String sql = "INSERT INTO alertes (stage_id, stagiaire_id, lue, date_creation) " +
                     "VALUES (?, ?, false, NOW())";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, stageId);
            ps.setLong(2, stagiaireId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean marquerCommeLue(Long alerteId) {
        String sql = "UPDATE alertes SET lue = true WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, alerteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean marquerToutesLues() {
        String sql = "UPDATE alertes SET lue = true WHERE lue = false";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean supprimerAlerte(Long alerteId) {
        String sql = "DELETE FROM alertes WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, alerteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public int genererAlertesAutomatiques(int joursAvance) {
        int count = 0;
        String sqlSelect = "SELECT st.id AS stage_id, st.stagiaire_id FROM stages st " +
                           "LEFT JOIN alertes a ON st.id = a.stage_id " +
                           "WHERE st.statut = 'En cours' " +
                           "AND DATEDIFF(st.date_fin, CURDATE()) <= ? " +
                           "AND st.date_fin >= CURDATE() AND a.id IS NULL";
        String sqlInsert = "INSERT INTO alertes (stage_id, stagiaire_id, lue, date_creation) VALUES (?, ?, false, NOW())";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement sel = c.prepareStatement(sqlSelect);
             PreparedStatement ins = c.prepareStatement(sqlInsert)) {
            sel.setInt(1, joursAvance);
            try (ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    ins.setLong(1, rs.getLong("stage_id"));
                    ins.setLong(2, rs.getLong("stagiaire_id"));
                    ins.addBatch();
                    count++;
                }
            }
            if (count > 0) ins.executeBatch();
        } catch (SQLException e) { e.printStackTrace(); }
        return count;
    }

    // ------------------------------------------------------------------ helpers

    private Alerte mapper(ResultSet rs) throws SQLException {
        Alerte a = new Alerte();
        a.setId(rs.getLong("id"));
        a.setStageId(rs.getLong("stage_id"));
        a.setStagiaireId(rs.getLong("stagiaire_id"));
        a.setNomStagiaire(rs.getString("nom_stagiaire"));
        a.setTypeStagiaire(rs.getString("type"));
        a.setNomEcole(rs.getString("nom_ecole"));
        a.setPays(rs.getString("pays"));
        a.setDateFinFormation(rs.getObject("date_fin", LocalDate.class));
        a.setLue(rs.getBoolean("lue"));
        a.setDateCreation(rs.getObject("date_creation", LocalDate.class));
        return a;
    }

    private Alerte mapperUrgent(ResultSet rs) throws SQLException {
        Alerte a = new Alerte();
        a.setStageId(rs.getLong("stage_id"));
        a.setStagiaireId(rs.getLong("stagiaire_id"));
        a.setNomStagiaire(rs.getString("nom_stagiaire"));
        a.setTypeStagiaire(rs.getString("type"));
        a.setNomEcole(rs.getString("nom_ecole"));
        a.setPays(rs.getString("pays"));
        a.setDateFinFormation(rs.getObject("date_fin", LocalDate.class));
        return a;
    }
}
