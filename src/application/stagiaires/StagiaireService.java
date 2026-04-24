package application.stagiaires;

import application.utils.DatabaseManager;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class StagiaireService {

    // ------------------------------------------------------------------ lecture

    public List<Stagiaire> getStagiairesActifs() {
        List<Stagiaire> list = new ArrayList<>();
        String sql = "SELECT * FROM stagiaires WHERE actif = true ORDER BY nom, prenom";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Stagiaire> getTousStagiaires() {
        List<Stagiaire> list = new ArrayList<>();
        String sql = "SELECT * FROM stagiaires ORDER BY nom, prenom";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapper(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Stagiaire> rechercherStagiaires(String terme) {
        List<Stagiaire> list = new ArrayList<>();
        String like = "%" + terme.toLowerCase() + "%";
        String sql = "SELECT * FROM stagiaires WHERE actif = true AND " +
                     "(LOWER(nom) LIKE ? OR LOWER(prenom) LIKE ? OR LOWER(matricule) LIKE ? " +
                     " OR LOWER(email) LIKE ?) ORDER BY nom, prenom";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like);
            ps.setString(3, like); ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Stagiaire getStagiaireParId(Long id) {
        String sql = "SELECT * FROM stagiaires WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Stagiaire getStagiaireParMatricule(String matricule) {
        String sql = "SELECT * FROM stagiaires WHERE matricule = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public int compterStagiairesActifs() {
        String sql = "SELECT COUNT(*) FROM stagiaires WHERE actif = true";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int compterStagiairesParEcole(Long ecoleId) {
        String sql = "SELECT COUNT(*) FROM stages WHERE ecole_id = ? AND statut = 'En cours'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ecoleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<String> getToutesSpecialites() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT specialite FROM stagiaires WHERE specialite IS NOT NULL AND specialite != '' ORDER BY specialite";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<String> getToutesLangues() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT langue FROM stagiaires WHERE langue IS NOT NULL AND langue != '' ORDER BY langue";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ------------------------------------------------------------------ écriture

    public boolean creerStagiaire(Stagiaire s) {
        String sql = "INSERT INTO stagiaires (matricule,nom,prenom,date_naissance,lieu_naissance," +
                     "nationalite,sexe,email,telephone,adresse,specialite,langue,niveau_etude," +
                     "diplome,type_formation,actif,date_inscription,photo,remarques) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, s);
            if (ps.executeUpdate() > 0) {
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) s.setId(gk.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean mettreAJourStagiaire(Stagiaire s) {
        String sql = "UPDATE stagiaires SET matricule=?,nom=?,prenom=?,date_naissance=?," +
                     "lieu_naissance=?,nationalite=?,sexe=?,email=?,telephone=?,adresse=?," +
                     "specialite=?,langue=?,niveau_etude=?,diplome=?,type_formation=?,actif=?," +
                     "date_inscription=?,photo=?,remarques=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, s);
            ps.setLong(20, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean supprimerStagiaire(Long id) {
        String sql = "UPDATE stagiaires SET actif = false WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public String genererMatricule() {
        String sql = "SELECT MAX(CAST(SUBSTRING(matricule, 3) AS UNSIGNED)) FROM stagiaires WHERE matricule LIKE 'ST%'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int max = rs.next() ? rs.getInt(1) : 0;
            return String.format("ST%06d", max + 1);
        } catch (SQLException e) {
            e.printStackTrace();
            return "ST" + Year.now().getValue() + String.format("%03d", (int)(Math.random() * 1000));
        }
    }

    // ------------------------------------------------------------------ helper

    private void remplir(PreparedStatement ps, Stagiaire s) throws SQLException {
        ps.setString(1, s.getMatricule());
        ps.setString(2, s.getNom());
        ps.setString(3, s.getPrenom());
        ps.setObject(4, s.getDateNaissance());
        ps.setString(5, s.getLieuNaissance());
        ps.setString(6, s.getNationalite());
        ps.setString(7, s.getSexe());
        ps.setString(8, s.getEmail());
        ps.setString(9, s.getTelephone());
        ps.setString(10, s.getAdresse());
        ps.setString(11, s.getSpecialite());
        ps.setString(12, s.getLangue());
        ps.setString(13, s.getNiveauEtude());
        ps.setString(14, s.getDiplome());
        ps.setString(15, s.getTypeFormation());
        ps.setBoolean(16, s.isActif());
        ps.setObject(17, s.getDateInscription());
        ps.setString(18, s.getPhoto());
        ps.setString(19, s.getRemarques());
    }

    private Stagiaire mapper(ResultSet rs) throws SQLException {
        Stagiaire s = new Stagiaire();
        s.setId(rs.getLong("id"));
        s.setMatricule(rs.getString("matricule"));
        s.setNom(rs.getString("nom"));
        s.setPrenom(rs.getString("prenom"));
        s.setDateNaissance(rs.getObject("date_naissance", java.time.LocalDate.class));
        s.setLieuNaissance(rs.getString("lieu_naissance"));
        s.setNationalite(rs.getString("nationalite"));
        s.setSexe(rs.getString("sexe"));
        s.setEmail(rs.getString("email"));
        s.setTelephone(rs.getString("telephone"));
        s.setAdresse(rs.getString("adresse"));
        s.setSpecialite(rs.getString("specialite"));
        s.setLangue(rs.getString("langue"));
        s.setNiveauEtude(rs.getString("niveau_etude"));
        s.setDiplome(rs.getString("diplome"));
        s.setTypeFormation(rs.getString("type_formation"));
        s.setActif(rs.getBoolean("actif"));
        s.setDateInscription(rs.getObject("date_inscription", java.time.LocalDate.class));
        s.setPhoto(rs.getString("photo"));
        s.setRemarques(rs.getString("remarques"));
        return s;
    }
}
