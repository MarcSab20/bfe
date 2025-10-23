package application;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EcoleService {
    
    // Obtenir toutes les écoles
    public List<Ecole> getToutesEcoles() {
        List<Ecole> ecoles = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM ecoles WHERE actif = true ORDER BY nom";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ecoles.add(mapResultSetToEcole(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ecoles;
    }
    
    // Obtenir les écoles par type
    public List<Ecole> getEcolesParType(String type) {
        List<Ecole> ecoles = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM ecoles WHERE type = ? AND actif = true ORDER BY nom";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, type);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ecoles.add(mapResultSetToEcole(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ecoles;
    }
    
    // Obtenir les écoles partenaires
    public List<Ecole> getEcolesPartenaires() {
        List<Ecole> ecoles = new ArrayList<>();
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM ecoles WHERE partenaire = true AND actif = true ORDER BY nom";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ecoles.add(mapResultSetToEcole(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ecoles;
    }
    
    // Obtenir une école par ID
    public Ecole getEcoleParId(Long id) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM ecoles WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Ecole ecole = mapResultSetToEcole(rs);
                rs.close();
                stmt.close();
                return ecole;
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    // Compter les écoles actives
    public int compterEcolesActives() {
        int count = 0;
        
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as total FROM ecoles WHERE actif = true";
            
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
    
    // Mapper ResultSet vers Ecole
    private Ecole mapResultSetToEcole(ResultSet rs) throws SQLException {
        Ecole ecole = new Ecole();
        ecole.setId(rs.getLong("id"));
        ecole.setNom(rs.getString("nom"));
        ecole.setType(rs.getString("type"));
        ecole.setPays(rs.getString("pays"));
        ecole.setVille(rs.getString("ville"));
        ecole.setAdresse(rs.getString("adresse"));
        ecole.setLatitude(rs.getObject("latitude", Double.class));
        ecole.setLongitude(rs.getObject("longitude", Double.class));
        ecole.setContact(rs.getString("contact"));
        ecole.setEmail(rs.getString("email"));
        ecole.setTelephone(rs.getString("telephone"));
        ecole.setSiteWeb(rs.getString("site_web"));
        
        // Convertir les spécialités (stockées comme chaîne séparée par des virgules)
        String specialitesStr = rs.getString("specialites");
        if (specialitesStr != null && !specialitesStr.isEmpty()) {
            ecole.setSpecialites(Arrays.asList(specialitesStr.split(",")));
        }
        
        ecole.setPartenaire(rs.getBoolean("partenaire"));
        ecole.setDatePartenariat(rs.getObject("date_partenariat", java.time.LocalDate.class));
        ecole.setDescription(rs.getString("description"));
        ecole.setActif(rs.getBoolean("actif"));
        
        return ecole;
    }
    
    // Créer une école
    public boolean creerEcole(Ecole ecole) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO ecoles (nom, type, pays, ville, adresse, latitude, longitude, " +
                        "contact, email, telephone, site_web, specialites, partenaire, date_partenariat, " +
                        "description, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, ecole.getNom());
            stmt.setString(2, ecole.getType());
            stmt.setString(3, ecole.getPays());
            stmt.setString(4, ecole.getVille());
            stmt.setString(5, ecole.getAdresse());
            stmt.setObject(6, ecole.getLatitude());
            stmt.setObject(7, ecole.getLongitude());
            stmt.setString(8, ecole.getContact());
            stmt.setString(9, ecole.getEmail());
            stmt.setString(10, ecole.getTelephone());
            stmt.setString(11, ecole.getSiteWeb());
            
            // Convertir la liste de spécialités en chaîne
            String specialites = ecole.getSpecialites() != null ? 
                String.join(",", ecole.getSpecialites()) : null;
            stmt.setString(12, specialites);
            
            stmt.setBoolean(13, ecole.isPartenaire());
            stmt.setObject(14, ecole.getDatePartenariat());
            stmt.setString(15, ecole.getDescription());
            stmt.setBoolean(16, ecole.isActif());
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}