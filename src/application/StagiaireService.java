package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StagiaireService {
    
    // Obtenir tous les stagiaires actifs
    public List<Stagiaire> getStagiairesActifs() {
        List<Stagiaire> stagiaires = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stagiaires WHERE actif = true ORDER BY nom, prenom";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                stagiaires.add(mapResultSetToStagiaire(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stagiaires;
    }
    
    // Obtenir un stagiaire par ID
    public Stagiaire getStagiaireParId(Long id) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM stagiaires WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Stagiaire stagiaire = mapResultSetToStagiaire(rs);
                rs.close();
                stmt.close();
                return stagiaire;
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    // Compter les stagiaires actifs
    public int compterStagiairesActifs() {
        int count = 0;
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as total FROM stagiaires WHERE actif = true";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
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
    
    // Compter les stagiaires par école
    public int compterStagiairesParEcole(Long ecoleId) {
        int count = 0;
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as total FROM stages " +
                        "WHERE ecole_id = ? AND statut = 'En cours'";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, ecoleId);
            
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
    
    // Obtenir toutes les spécialités
    public List<String> getToutesSpecialites() {
        List<String> specialites = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT DISTINCT specialite FROM stagiaires " +
                        "WHERE specialite IS NOT NULL AND specialite != '' " +
                        "ORDER BY specialite";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                specialites.add(rs.getString("specialite"));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return specialites;
    }
    
    // Obtenir toutes les langues
    public List<String> getToutesLangues() {
        List<String> langues = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT DISTINCT langue FROM stagiaires " +
                        "WHERE langue IS NOT NULL AND langue != '' " +
                        "ORDER BY langue";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                langues.add(rs.getString("langue"));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return langues;
    }
    
    // Mapper ResultSet vers Stagiaire
    private Stagiaire mapResultSetToStagiaire(ResultSet rs) throws SQLException {
        Stagiaire stagiaire = new Stagiaire();
        stagiaire.setId(rs.getLong("id"));
        stagiaire.setMatricule(rs.getString("matricule"));
        stagiaire.setNom(rs.getString("nom"));
        stagiaire.setPrenom(rs.getString("prenom"));
        stagiaire.setDateNaissance(rs.getObject("date_naissance", java.time.LocalDate.class));
        stagiaire.setLieuNaissance(rs.getString("lieu_naissance"));
        stagiaire.setNationalite(rs.getString("nationalite"));
        stagiaire.setSexe(rs.getString("sexe"));
        stagiaire.setEmail(rs.getString("email"));
        stagiaire.setTelephone(rs.getString("telephone"));
        stagiaire.setAdresse(rs.getString("adresse"));
        stagiaire.setSpecialite(rs.getString("specialite"));
        stagiaire.setLangue(rs.getString("langue"));
        stagiaire.setNiveauEtude(rs.getString("niveau_etude"));
        stagiaire.setDiplome(rs.getString("diplome"));
        stagiaire.setTypeFormation(rs.getString("type_formation"));
        stagiaire.setActif(rs.getBoolean("actif"));
        stagiaire.setDateInscription(rs.getObject("date_inscription", java.time.LocalDate.class));
        stagiaire.setPhoto(rs.getString("photo"));
        stagiaire.setRemarques(rs.getString("remarques"));
        
        return stagiaire;
    }
    
    // Créer un stagiaire
    public boolean creerStagiaire(Stagiaire stagiaire) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO stagiaires (matricule, nom, prenom, date_naissance, lieu_naissance, " +
                        "nationalite, sexe, email, telephone, adresse, specialite, langue, niveau_etude, " +
                        "diplome, type_formation, actif, date_inscription, photo, remarques) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, stagiaire.getMatricule());
            stmt.setString(2, stagiaire.getNom());
            stmt.setString(3, stagiaire.getPrenom());
            stmt.setObject(4, stagiaire.getDateNaissance());
            stmt.setString(5, stagiaire.getLieuNaissance());
            stmt.setString(6, stagiaire.getNationalite());
            stmt.setString(7, stagiaire.getSexe());
            stmt.setString(8, stagiaire.getEmail());
            stmt.setString(9, stagiaire.getTelephone());
            stmt.setString(10, stagiaire.getAdresse());
            stmt.setString(11, stagiaire.getSpecialite());
            stmt.setString(12, stagiaire.getLangue());
            stmt.setString(13, stagiaire.getNiveauEtude());
            stmt.setString(14, stagiaire.getDiplome());
            stmt.setString(15, stagiaire.getTypeFormation());
            stmt.setBoolean(16, stagiaire.isActif());
            stmt.setObject(17, stagiaire.getDateInscription());
            stmt.setString(18, stagiaire.getPhoto());
            stmt.setString(19, stagiaire.getRemarques());
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}