package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AlerteService {
    
    // Obtenir toutes les alertes actives
    public List<Alerte> getAlertesActives() {
        List<Alerte> alertes = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT a.*, s.nom AS nom_stagiaire, s.prenom, st.type AS type_stage, " +
                        "e.nom AS nom_ecole, e.pays " +
                        "FROM alertes a " +
                        "JOIN stages st ON a.stage_id = st.id " +
                        "JOIN stagiaires s ON st.stagiaire_id = s.id " +
                        "JOIN ecoles e ON st.ecole_id = e.id " +
                        "WHERE a.lue = false AND st.date_fin >= CURDATE() " +
                        "ORDER BY st.date_fin ASC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Alerte alerte = new Alerte();
                alerte.setId(rs.getLong("id"));
                alerte.setStageId(rs.getLong("stage_id"));
                alerte.setStagiaireId(rs.getLong("stagiaire_id"));
                alerte.setNomStagiaire(rs.getString("nom_stagiaire") + " " + rs.getString("prenom"));
                alerte.setTypeStagiaire(rs.getString("type_stage"));
                alerte.setNomEcole(rs.getString("nom_ecole"));
                alerte.setPays(rs.getString("pays"));
                alerte.setDateFinFormation(rs.getObject("date_fin", LocalDate.class));
                alerte.setLue(rs.getBoolean("lue"));
                
                alertes.add(alerte);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return alertes;
    }
    
    // Compter les retours urgents (moins de X jours)
    public int compterRetoursUrgents(int joursLimite) {
        int count = 0;
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as total FROM stages " +
                        "WHERE statut = 'En cours' " +
                        "AND DATEDIFF(date_fin, CURDATE()) <= ? " +
                        "AND date_fin >= CURDATE()";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, joursLimite);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return count;
    }
    
    // Obtenir les alertes urgentes (moins de X jours)
    public List<Alerte> getAlertesUrgentes(int joursLimite) {
        List<Alerte> alertes = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT st.id AS stage_id, s.id AS stagiaire_id, " +
                        "CONCAT(s.nom, ' ', s.prenom) AS nom_stagiaire, " +
                        "st.date_fin, e.nom AS nom_ecole " +
                        "FROM stages st " +
                        "JOIN stagiaires s ON st.stagiaire_id = s.id " +
                        "JOIN ecoles e ON st.ecole_id = e.id " +
                        "WHERE st.statut = 'En cours' " +
                        "AND DATEDIFF(st.date_fin, CURDATE()) <= ? " +
                        "AND st.date_fin >= CURDATE() " +
                        "ORDER BY st.date_fin ASC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, joursLimite);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Alerte alerte = new Alerte();
                alerte.setStageId(rs.getLong("stage_id"));
                alerte.setStagiaireId(rs.getLong("stagiaire_id"));
                alerte.setNomStagiaire(rs.getString("nom_stagiaire"));
                alerte.setNomEcole(rs.getString("nom_ecole"));
                alerte.setDateFinFormation(rs.getObject("date_fin", LocalDate.class));
                
                alertes.add(alerte);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return alertes;
    }
    
    // Créer une alerte
    public boolean creerAlerte(Long stageId, Long stagiaireId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO alertes (stage_id, stagiaire_id, lue, date_creation) " +
                        "VALUES (?, ?, false, NOW())";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, stageId);
            stmt.setLong(2, stagiaireId);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Marquer une alerte comme lue
    public boolean marquerCommeLue(Long alerteId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE alertes SET lue = true, date_modification = NOW() WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, alerteId);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}