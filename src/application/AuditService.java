package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuditService {
    
    // Enregistrer un événement
    public boolean enregistrerEvenement(Long utilisateurId, String typeEvenement, 
                                       String description, String details, LocalDateTime dateHeure) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO audit_logs (utilisateur_id, type_evenement, description, details, date_heure) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setObject(1, utilisateurId);
            stmt.setString(2, typeEvenement);
            stmt.setString(3, description);
            stmt.setString(4, details);
            stmt.setObject(5, dateHeure);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Enregistrer un événement sans utilisateur (pour les actions anonymes)
    public boolean enregistrerEvenement(String typeEvenement, String description, String details) {
        return enregistrerEvenement(null, typeEvenement, description, details, LocalDateTime.now());
    }
}