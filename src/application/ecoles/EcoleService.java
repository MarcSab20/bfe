package application.ecoles;

import application.utils.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EcoleService {

    // ------------------------------------------------------------------ lecture

    public List<Ecole> getToutesEcoles() {
        List<Ecole> ecoles = new ArrayList<>();
        String sql = "SELECT * FROM ecoles WHERE actif = true ORDER BY nom";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ecoles.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return ecoles;
    }

    public List<Ecole> getEcolesParType(String type) {
        List<Ecole> ecoles = new ArrayList<>();
        String sql = "SELECT * FROM ecoles WHERE type = ? AND actif = true ORDER BY nom";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ecoles.add(mapper(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ecoles;
    }

    public List<Ecole> getEcolesPartenaires() {
        List<Ecole> ecoles = new ArrayList<>();
        String sql = "SELECT * FROM ecoles WHERE partenaire = true AND actif = true ORDER BY nom";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ecoles.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return ecoles;
    }

    public List<Ecole> rechercherEcoles(String terme) {
        List<Ecole> ecoles = new ArrayList<>();
        String sql = "SELECT * FROM ecoles WHERE actif = true AND " +
                     "(LOWER(nom) LIKE ? OR LOWER(pays) LIKE ? OR LOWER(ville) LIKE ?) ORDER BY nom";
        String like = "%" + terme.toLowerCase() + "%";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ecoles.add(mapper(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ecoles;
    }

    public Ecole getEcoleParId(Long id) {
        String sql = "SELECT * FROM ecoles WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public int compterEcolesActives() {
        String sql = "SELECT COUNT(*) FROM ecoles WHERE actif = true";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<String> getTousPays() {
        List<String> pays = new ArrayList<>();
        String sql = "SELECT DISTINCT pays FROM ecoles WHERE pays IS NOT NULL AND actif = true ORDER BY pays";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) pays.add(rs.getString(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return pays;
    }

    // ------------------------------------------------------------------ écriture

    public boolean creerEcole(Ecole e) {
        String sql = "INSERT INTO ecoles (nom,type,pays,ville,adresse,latitude,longitude," +
                     "contact,email,telephone,site_web,specialites,partenaire,date_partenariat," +
                     "description,actif) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, e);
            if (ps.executeUpdate() > 0) {
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) e.setId(gk.getLong(1));
                }
                return true;
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return false;
    }

    public boolean mettreAJourEcole(Ecole e) {
        String sql = "UPDATE ecoles SET nom=?,type=?,pays=?,ville=?,adresse=?,latitude=?," +
                     "longitude=?,contact=?,email=?,telephone=?,site_web=?,specialites=?," +
                     "partenaire=?,date_partenariat=?,description=?,actif=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, e);
            ps.setLong(17, e.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); }
        return false;
    }

    public boolean supprimerEcole(Long id) {
        String sql = "UPDATE ecoles SET actif = false WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private void remplir(PreparedStatement ps, Ecole e) throws SQLException {
        ps.setString(1, e.getNom());
        ps.setString(2, e.getType());
        ps.setString(3, e.getPays());
        ps.setString(4, e.getVille());
        ps.setString(5, e.getAdresse());
        ps.setObject(6, e.getLatitude());
        ps.setObject(7, e.getLongitude());
        ps.setString(8, e.getContact());
        ps.setString(9, e.getEmail());
        ps.setString(10, e.getTelephone());
        ps.setString(11, e.getSiteWeb());
        ps.setString(12, e.getSpecialites() != null ? String.join(",", e.getSpecialites()) : null);
        ps.setBoolean(13, e.isPartenaire());
        ps.setObject(14, e.getDatePartenariat());
        ps.setString(15, e.getDescription());
        ps.setBoolean(16, e.isActif());
    }

    private Ecole mapper(ResultSet rs) throws SQLException {
        Ecole e = new Ecole();

        e.setId(rs.getLong("id"));
        e.setNom(rs.getString("nom"));
        e.setType(rs.getString("type"));
        e.setPays(rs.getString("pays"));
        e.setVille(rs.getString("ville"));
        e.setAdresse(rs.getString("adresse"));

        // ✅ correction DECIMAL → Double
        BigDecimal lat = rs.getBigDecimal("latitude");
        if (lat != null) e.setLatitude(lat.doubleValue());

        BigDecimal lon = rs.getBigDecimal("longitude");
        if (lon != null) e.setLongitude(lon.doubleValue());

        e.setContact(rs.getString("contact"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setSiteWeb(rs.getString("site_web"));

        String sp = rs.getString("specialites");
        if (sp != null && !sp.isEmpty()) {
            e.setSpecialites(new ArrayList<>(Arrays.asList(sp.split(","))));
        }

        e.setPartenaire(rs.getBoolean("partenaire"));

        // ✅ correction DATE
        Date date = rs.getDate("date_partenariat");
        if (date != null) {
            e.setDatePartenariat(date.toLocalDate());
        }

        e.setDescription(rs.getString("description"));
        e.setActif(rs.getBoolean("actif"));

        return e;
    }
}
