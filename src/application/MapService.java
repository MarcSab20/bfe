package application;

import java.sql.*;
import java.util.*;

/**
 * Service pour la gestion de la carte
 */
public class MapService {
    
    /**
     * Récupère tous les marqueurs pour la carte
     */
    public List<MapMarker> getMarkers() {
        List<MapMarker> markers = new ArrayList<>();
        
        // Ajouter les écoles
        markers.addAll(getEcoleMarkers());
        
        // Ajouter les stagiaires
        markers.addAll(getStagiaireMarkers());
        
        return markers;
    }
    
    /**
     * Récupère les marqueurs des écoles
     */
    private List<MapMarker> getEcoleMarkers() {
        List<MapMarker> markers = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT e.*, COUNT(s.id) as nb_stagiaires " +
                 "FROM ecoles e " +
                 "LEFT JOIN stagiaires s ON e.id = s.ecole_id AND s.statut = 'En cours' " +
                 "GROUP BY e.id")) {
            
            while (rs.next()) {
                MapMarker marker = new MapMarker();
                marker.setId(rs.getString("id"));
                marker.setType("ecole");
                marker.setNom(rs.getString("nom"));
                marker.setLat(rs.getDouble("latitude"));
                marker.setLon(rs.getDouble("longitude"));
                marker.setPays(rs.getString("pays"));
                marker.setVille(rs.getString("ville"));
                marker.setTypeEcole(rs.getString("type"));
                marker.setNbStagiaires(rs.getInt("nb_stagiaires"));
                
                markers.add(marker);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return markers;
    }
    
    /**
     * Récupère les marqueurs des stagiaires
     */
    private List<MapMarker> getStagiaireMarkers() {
        List<MapMarker> markers = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT s.*, e.nom as ecole_nom " +
                 "FROM stagiaires s " +
                 "JOIN ecoles e ON s.ecole_id = e.id " +
                 "WHERE s.statut = 'En cours'")) {
            
            while (rs.next()) {
                MapMarker marker = new MapMarker();
                marker.setId(rs.getString("id"));
                marker.setType("stagiaire");
                marker.setNom(rs.getString("nom") + " " + rs.getString("prenom"));
                marker.setLat(rs.getDouble("latitude"));
                marker.setLon(rs.getDouble("longitude"));
                marker.setPays(rs.getString("pays"));
                marker.setVille(rs.getString("ville"));
                marker.setSpecialite(rs.getString("specialite"));
                marker.setEcoleNom(rs.getString("ecole_nom"));
                marker.setStatut(rs.getString("statut"));
                marker.setLangue(rs.getString("langue"));
                
                markers.add(marker);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return markers;
    }
    
    /**
     * Récupère une école par son ID
     */
    public Ecole getEcoleById(String id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM ecoles WHERE id = ?")) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Ecole ecole = new Ecole();
                ecole.setId(rs.getString("id"));
                ecole.setNom(rs.getString("nom"));
                ecole.setType(rs.getString("type"));
                ecole.setVille(rs.getString("ville"));
                ecole.setPays(rs.getString("pays"));
                ecole.setLatitude(rs.getDouble("latitude"));
                ecole.setLongitude(rs.getDouble("longitude"));
                ecole.setEmail(rs.getString("email"));
                ecole.setTelephone(rs.getString("telephone"));
                ecole.setAdresse(rs.getString("adresse"));
                
                // Récupérer le nombre de stagiaires
                try (Statement stmt = conn.createStatement();
                     ResultSet rs2 = stmt.executeQuery(
                         "SELECT COUNT(*) FROM stagiaires WHERE ecole_id = '" + id + "' AND statut = 'En cours'")) {
                    if (rs2.next()) {
                        ecole.setNbStagiairesActuels(rs2.getInt(1));
                    }
                }
                
                // Récupérer les spécialités
                List<String> specialites = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs3 = stmt.executeQuery(
                         "SELECT specialite FROM ecole_specialites WHERE ecole_id = '" + id + "'")) {
                    while (rs3.next()) {
                        specialites.add(rs3.getString("specialite"));
                    }
                }
                ecole.setSpecialites(specialites);
                
                return ecole;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Récupère un stagiaire par son ID
     */
    public Stagiaire getStagiaireById(String id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT s.*, e.nom as ecole_nom " +
                 "FROM stagiaires s " +
                 "JOIN ecoles e ON s.ecole_id = e.id " +
                 "WHERE s.id = ?")) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Stagiaire stagiaire = new Stagiaire();
                stagiaire.setId(rs.getString("id"));
                stagiaire.setNom(rs.getString("nom"));
                stagiaire.setPrenom(rs.getString("prenom"));
                stagiaire.setSpecialite(rs.getString("specialite"));
                stagiaire.setEcoleId(rs.getString("ecole_id"));
                stagiaire.setEcoleNom(rs.getString("ecole_nom"));
                stagiaire.setPays(rs.getString("pays"));
                stagiaire.setVille(rs.getString("ville"));
                stagiaire.setLatitude(rs.getDouble("latitude"));
                stagiaire.setLongitude(rs.getDouble("longitude"));
                stagiaire.setDateDebut(rs.getDate("date_debut").toLocalDate());
                stagiaire.setDateFin(rs.getDate("date_fin").toLocalDate());
                stagiaire.setLangue(rs.getString("langue"));
                stagiaire.setStatut(rs.getString("statut"));
                stagiaire.setTypeStage(rs.getString("type_stage"));
                stagiaire.setDocumentId(rs.getString("document_id"));
                
                return stagiaire;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
}