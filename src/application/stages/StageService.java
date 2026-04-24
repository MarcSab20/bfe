package application.services;

import application.models.StageFormation;
import application.models.Stagiaire;
import application.models.Ecole;
import application.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des stages
 */
public class StageService {
    
    private EcoleService ecoleService;
    private StagiaireService stagiaireService;
    
    public StageService() {
        this.ecoleService = new EcoleService();
        this.stagiaireService = new StagiaireService();
    }
    
    /**
     * Obtenir tous les stages
     */
    public List<StageFormation> getTousLesStages() {
        List<StageFormation> stages = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stages ORDER BY date_creation DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                stages.add(mapResultSetToStage(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stages;
    }
    
    /**
     * Obtenir les stages en cours
     */
    public List<StageFormation> getStagesEnCours() {
        List<StageFormation> stages = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stages WHERE statut = 'En cours' ORDER BY date_debut DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                stages.add(mapResultSetToStage(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stages;
    }
    
    /**
     * Obtenir le stage actif d'un stagiaire
     */
    public StageFormation getStageActifByStagiaire(Long stagiaireId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stages WHERE stagiaire_id = ? AND statut = 'En cours'";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, stagiaireId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                StageFormation stage = mapResultSetToStage(rs);
                rs.close();
                stmt.close();
                return stage;
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtenir un stage par ID
     */
    public StageFormation getStageParId(Long id) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stages WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                StageFormation stage = mapResultSetToStage(rs);
                rs.close();
                stmt.close();
                return stage;
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtenir les stages par école
     */
    public List<StageFormation> getStagesParEcole(Long ecoleId) {
        List<StageFormation> stages = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stages WHERE ecole_id = ? ORDER BY date_debut DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, ecoleId);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                stages.add(mapResultSetToStage(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stages;
    }
    
    /**
     * Compter les stages par statut
     */
    public int compterStagesParStatut(String statut) {
        int count = 0;
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as total FROM stages WHERE statut = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, statut);
            
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
    
    /**
     * Mapper ResultSet vers Stage
     */
    private StageFormation mapResultSetToStage(ResultSet rs) throws SQLException {
        StageFormation stage = new StageFormation();
        stage.setId(rs.getLong("id"));
        
        // Charger le stagiaire
        Long stagiaireId = rs.getLong("stagiaire_id");
        Stagiaire stagiaire = stagiaireService.getStagiaireParId(stagiaireId);
        stage.setStagiaire(stagiaire);
        
        // Charger l'école
        Long ecoleId = rs.getLong("ecole_id");
        Ecole ecole = ecoleService.getEcoleParId(ecoleId);
        stage.setEcole(ecole);
        
        stage.setType(rs.getString("type"));
        stage.setDateDebut(rs.getObject("date_debut", java.time.LocalDate.class));
        stage.setDateFin(rs.getObject("date_fin", java.time.LocalDate.class));
        stage.setSpecialite(rs.getString("specialite"));
        stage.setEncadrant(rs.getString("encadrant"));
        stage.setTuteur(rs.getString("tuteur"));
        stage.setObjectifs(rs.getString("objectifs"));
        stage.setDescription(rs.getString("description"));
        stage.setStatut(rs.getString("statut"));
        stage.setDocumentId(rs.getString("document_id"));
        stage.setDocumentNom(rs.getString("document_nom"));
        stage.setRemarques(rs.getString("remarques"));
        stage.setDateCreation(rs.getObject("date_creation", java.time.LocalDate.class));
        
        return stage;
    }
    
    /**
     * Créer un stage
     */
    public boolean creerStage(StageFormation stage) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO stages (stagiaire_id, ecole_id, type, date_debut, date_fin, " +
                        "specialite, encadrant, tuteur, objectifs, description, statut, document_id, " +
                        "document_nom, remarques, date_creation) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, stage.getStagiaire().getId());
            stmt.setLong(2, stage.getEcole().getId());
            stmt.setString(3, stage.getType());
            stmt.setObject(4, stage.getDateDebut());
            stmt.setObject(5, stage.getDateFin());
            stmt.setString(6, stage.getSpecialite());
            stmt.setString(7, stage.getEncadrant());
            stmt.setString(8, stage.getTuteur());
            stmt.setString(9, stage.getObjectifs());
            stmt.setString(10, stage.getDescription());
            stmt.setString(11, stage.getStatut());
            stmt.setString(12, stage.getDocumentId());
            stmt.setString(13, stage.getDocumentNom());
            stmt.setString(14, stage.getRemarques());
            stmt.setObject(15, stage.getDateCreation());
            
            int result = stmt.executeUpdate();
            
            // Récupérer l'ID généré
            if (result > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    stage.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();
            }
            
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Mettre à jour un stage
     */
    public boolean mettreAJourStage(StageFormation stage) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE stages SET stagiaire_id = ?, ecole_id = ?, type = ?, date_debut = ?, " +
                        "date_fin = ?, specialite = ?, encadrant = ?, tuteur = ?, objectifs = ?, " +
                        "description = ?, statut = ?, document_id = ?, document_nom = ?, remarques = ? " +
                        "WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, stage.getStagiaire().getId());
            stmt.setLong(2, stage.getEcole().getId());
            stmt.setString(3, stage.getType());
            stmt.setObject(4, stage.getDateDebut());
            stmt.setObject(5, stage.getDateFin());
            stmt.setString(6, stage.getSpecialite());
            stmt.setString(7, stage.getEncadrant());
            stmt.setString(8, stage.getTuteur());
            stmt.setString(9, stage.getObjectifs());
            stmt.setString(10, stage.getDescription());
            stmt.setString(11, stage.getStatut());
            stmt.setString(12, stage.getDocumentId());
            stmt.setString(13, stage.getDocumentNom());
            stmt.setString(14, stage.getRemarques());
            stmt.setLong(15, stage.getId());
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Mettre à jour le statut d'un stage
     */
    public boolean mettreAJourStatut(Long stageId, String nouveauStatut) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE stages SET statut = ? WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nouveauStatut);
            stmt.setLong(2, stageId);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Mettre à jour le document d'un stage
     */
    public boolean mettreAJourDocument(Long stageId, String documentId, String documentNom) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE stages SET document_id = ?, document_nom = ? WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, documentId);
            stmt.setString(2, documentNom);
            stmt.setLong(3, stageId);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Supprimer un stage
     */
    public boolean supprimerStage(Long id) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "DELETE FROM stages WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}