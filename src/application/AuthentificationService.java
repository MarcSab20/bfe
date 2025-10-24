package application;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuthentificationService {
    
    // Authentifier un utilisateur
    public Utilisateur authentifier(String nomUtilisateur, String motDePasse, String niveauAcces) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM utilisateurs WHERE nom_utilisateur = ? AND niveau_acces = ? AND actif = true";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nomUtilisateur);
            stmt.setString(2, niveauAcces);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String motDePasseHash = rs.getString("mot_de_passe_hash");
                
                // Vérifier le mot de passe
                if (verifierMotDePasse(motDePasse, motDePasseHash)) {
                    Utilisateur utilisateur = new Utilisateur();
                    utilisateur.setId(rs.getLong("id"));
                    utilisateur.setNomUtilisateur(rs.getString("nom_utilisateur"));
                    utilisateur.setNiveauAcces(rs.getString("niveau_acces"));
                    utilisateur.setNom(rs.getString("nom"));
                    utilisateur.setPrenom(rs.getString("prenom"));
                    utilisateur.setEmail(rs.getString("email"));
                    
                    // Mettre à jour la dernière connexion
                    mettreAJourDerniereConnexion(utilisateur.getId());
                    
                    return utilisateur;
                }
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
//    // Hacher un mot de passe (SHA-256)
//    public static String hacherMotDePasse(String motDePasse) {
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            byte[] hash = md.digest(motDePasse.getBytes("UTF-8"));
//            
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : hash) {
//                String hex = Integer.toHexString(0xff & b);
//                if (hex.length() == 1) hexString.append('0');
//                hexString.append(hex);
//            }
//            
//            return hexString.toString();
//            
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
    
    // Vérifier un mot de passe
    private boolean verifierMotDePasse(String motDePasse, String hash) {
        String motDePasseHashe = motDePasse;
        return motDePasseHashe != null && motDePasseHashe.equals(hash);
    }
    
    // Mettre à jour la dernière connexion
    private void mettreAJourDerniereConnexion(Long utilisateurId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE utilisateurs SET derniere_connexion = ? WHERE id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setObject(1, LocalDateTime.now());
            stmt.setLong(2, utilisateurId);
            
            stmt.executeUpdate();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Créer un utilisateur (pour l'initialisation)
    public boolean creerUtilisateur(String nomUtilisateur, String motDePasse, String niveauAcces, 
                                    String nom, String prenom, String email) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO utilisateurs (nom_utilisateur, mot_de_passe_hash, niveau_acces, nom, prenom, email, actif) " +
                        "VALUES (?, ?, ?, ?, ?, ?, true)";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nomUtilisateur);
            stmt.setString(2, motDePasse);
            stmt.setString(3, niveauAcces);
            stmt.setString(4, nom);
            stmt.setString(5, prenom);
            stmt.setString(6, email);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}