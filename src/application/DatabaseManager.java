package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static Connection connection;
    private static final String URL = "jdbc:mysql://localhost:3306/bfe";
    private static final String USER = "marco";
    private static final String PASSWORD = "29Papa278."; 
    
    // Initialiser la connexion
    public static void initialize() {
        try {
            // Charger le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Propriétés de connexion
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("useSSL", "false");
            props.setProperty("serverTimezone", "UTC");
            props.setProperty("allowPublicKeyRetrieval", "true");
            
            // Établir la connexion
            connection = DriverManager.getConnection(URL, props);
            
            System.out.println("Connexion à la base de données MySQL réussie");
            
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL introuvable: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Obtenir la connexion
    public static Connection getConnection() {
        try {
            // Vérifier si la connexion est toujours valide
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            initialize();
        }
        return connection;
    }
    
    // Fermer la connexion
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion MySQL fermée");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Tester la connexion
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}