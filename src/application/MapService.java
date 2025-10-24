package application;

import java.sql.*;
import java.sql.Date;
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
                 "SELECT e.*, COUNT(st.id) as nb_stagiaires " +
                 "FROM ecoles e " +
                 "LEFT JOIN stages st ON e.id = st.ecole_id AND st.statut = 'En cours' " +
                 "WHERE e.actif = true " +
                 "GROUP BY e.id")) {
            
            while (rs.next()) {
                MapMarker marker = new MapMarker();
                marker.setId(String.valueOf(rs.getLong("id")));
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
                 "SELECT s.id, s.nom, s.prenom, s.specialite, s.langue, " +
                 "st.statut, e.nom as ecole_nom, e.pays, e.ville, e.latitude, e.longitude " +
                 "FROM stagiaires s " +
                 "JOIN stages st ON s.id = st.stagiaire_id " +
                 "JOIN ecoles e ON st.ecole_id = e.id " +
                 "WHERE st.statut = 'En cours' AND s.actif = true")) {
            
            while (rs.next()) {
                MapMarker marker = new MapMarker();
                marker.setId(String.valueOf(rs.getLong("id")));
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
            
            pstmt.setLong(1, Long.parseLong(id));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Ecole ecole = new Ecole();
                ecole.setId(rs.getLong("id"));
                ecole.setNom(rs.getString("nom"));
                ecole.setType(rs.getString("type"));
                ecole.setVille(rs.getString("ville"));
                ecole.setPays(rs.getString("pays"));
                ecole.setLatitude(rs.getDouble("latitude"));
                ecole.setLongitude(rs.getDouble("longitude"));
                ecole.setEmail(rs.getString("email"));
                ecole.setTelephone(rs.getString("telephone"));
                ecole.setAdresse(rs.getString("adresse"));
                ecole.setActif(rs.getBoolean("actif"));
                
                // Récupérer le nombre de stagiaires
                try (Statement stmt = conn.createStatement();
                     ResultSet rs2 = stmt.executeQuery(
                         "SELECT COUNT(*) as total FROM stages " +
                         "WHERE ecole_id = " + id + " AND statut = 'En cours'")) {
                    if (rs2.next()) {
                        ecole.setNbStagiairesActuels(rs2.getInt("total"));
                    }
                }
                
                // Récupérer les spécialités
                String specialitesStr = rs.getString("specialites");
                if (specialitesStr != null && !specialitesStr.isEmpty()) {
                    ecole.setSpecialites(Arrays.asList(specialitesStr.split(",")));
                }
                
                return ecole;
            }
            
        } catch (SQLException | NumberFormatException e) {
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
                 "SELECT s.*, st.statut, st.type as type_stage, st.date_debut, st.date_fin, " +
                 "st.document_id, e.id as ecole_id, e.nom as ecole_nom, e.pays, e.ville, " +
                 "e.latitude, e.longitude " +
                 "FROM stagiaires s " +
                 "LEFT JOIN stages st ON s.id = st.stagiaire_id AND st.statut = 'En cours' " +
                 "LEFT JOIN ecoles e ON st.ecole_id = e.id " +
                 "WHERE s.id = ?")) {
            
            pstmt.setLong(1, Long.parseLong(id));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Stagiaire stagiaire = new Stagiaire();
                stagiaire.setId(rs.getLong("id"));
                stagiaire.setNom(rs.getString("nom"));
                stagiaire.setPrenom(rs.getString("prenom"));
                stagiaire.setMatricule(rs.getString("matricule"));
                stagiaire.setSpecialite(rs.getString("specialite"));
                stagiaire.setEmail(rs.getString("email"));
                stagiaire.setTelephone(rs.getString("telephone"));
                stagiaire.setLangue(rs.getString("langue"));
                stagiaire.setActif(rs.getBoolean("actif"));
                
                // Informations du stage actif
                Long ecoleIdValue = rs.getObject("ecole_id", Long.class);
                if (ecoleIdValue != null) {
                    stagiaire.setEcoleId(String.valueOf(ecoleIdValue));
                    stagiaire.setEcoleNom(rs.getString("ecole_nom"));
                    stagiaire.setPays(rs.getString("pays"));
                    stagiaire.setVille(rs.getString("ville"));
                    stagiaire.setLatitude(rs.getObject("latitude", Double.class));
                    stagiaire.setLongitude(rs.getObject("longitude", Double.class));
                    
                    Date dateDebut = rs.getDate("date_debut");
                    if (dateDebut != null) {
                        stagiaire.setDateDebut(dateDebut.toLocalDate());
                    }
                    
                    Date dateFin = rs.getDate("date_fin");
                    if (dateFin != null) {
                        stagiaire.setDateFin(dateFin.toLocalDate());
                    }
                    
                    stagiaire.setStatut(rs.getString("statut"));
                    stagiaire.setTypeStage(rs.getString("type_stage"));
                    stagiaire.setDocumentId(rs.getString("document_id"));
                }
                
                return stagiaire;
            }
            
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        
        return null;
    }
}