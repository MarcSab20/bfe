package application.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Gestionnaire de connexion à la base de données.
 * Version SAFE : 1 connexion = 1 appel (thread-safe).
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private static final String CONFIG_FILE = "/database.properties";

    private static String url;
    private static String username;
    private static String password;
    private static String driver;
    private static boolean initialized = false;

    private DatabaseManager() {}

    // -------------------------------------------------- INITIALISATION

    private static synchronized void init() {
        if (initialized) return;

        Properties props = new Properties();

        try (InputStream is = DatabaseManager.class.getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            } else {
                LOG.warning("database.properties introuvable – utilisation des valeurs par défaut");
                setDefaults(props);
            }
        } catch (IOException e) {
            LOG.severe("Erreur lecture database.properties : " + e.getMessage());
            setDefaults(props);
        }

        driver   = props.getProperty("db.driver",   "com.mysql.cj.jdbc.Driver");
        url      = props.getProperty("db.url",      "jdbc:mysql://localhost:3306/bfe?useSSL=false&serverTimezone=UTC");
        username = props.getProperty("db.username", "marco");
        password = props.getProperty("db.password", "");

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            LOG.severe("Driver JDBC introuvable : " + driver);
        }

        initialized = true;
    }

    private static void setDefaults(Properties props) {
        props.setProperty("db.driver",   "com.mysql.cj.jdbc.Driver");
        props.setProperty("db.url",      "jdbc:mysql://localhost:3306/bfe?useSSL=false&serverTimezone=UTC");
        props.setProperty("db.username", "root");
        props.setProperty("db.password", "");
    }

    // -------------------------------------------------- CONNEXION

    /**
     * Retourne UNE NOUVELLE connexion à chaque appel (thread-safe).
     */
    public static Connection getConnection() throws SQLException {
        init();
        Connection connection = DriverManager.getConnection(url, username, password);
        LOG.info("Nouvelle connexion établie vers : " + url);
        return connection;
    }

    /**
     * Alias (optionnel) pour plus de clarté.
     */
    public static Connection newConnection() throws SQLException {
        return getConnection();
    }

    // -------------------------------------------------- UTILITAIRES

    /**
     * Teste si la connexion fonctionne.
     */
    public static boolean testerConnexion() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            LOG.warning("Test de connexion échoué : " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------- GETTERS CONFIG

    public static String getUrl() {
        init();
        return url;
    }

    public static String getUsername() {
        init();
        return username;
    }

    public static String getDriver() {
        init();
        return driver;
    }
}