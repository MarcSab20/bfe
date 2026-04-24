package application.ecoles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Ecole {

    private Long id;
    private String nom;
    private String type;
    private String pays;
    private String ville;
    private String adresse;
    private Double latitude;
    private Double longitude;
    private String contact;
    private String email;
    private String telephone;
    private String siteWeb;
    private List<String> specialites = new ArrayList<>();
    private boolean partenaire;
    private LocalDate datePartenariat;
    private String description;
    private boolean actif = true;

    public Ecole() {}

    public Ecole(String nom, String type, String pays, String ville) {
        this.nom = nom;
        this.type = type;
        this.pays = pays;
        this.ville = ville;
        this.actif = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public List<String> getSpecialites() { return specialites; }
    public void setSpecialites(List<String> specialites) {
        this.specialites = specialites != null ? specialites : new ArrayList<>();
    }

    public boolean isPartenaire() { return partenaire; }
    public void setPartenaire(boolean partenaire) { this.partenaire = partenaire; }

    public LocalDate getDatePartenariat() { return datePartenariat; }
    public void setDatePartenariat(LocalDate datePartenariat) { this.datePartenariat = datePartenariat; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public String getSpecialitesAsString() {
        return specialites != null ? String.join(", ", specialites) : "";
    }

    public String getNomComplet() {
        StringBuilder sb = new StringBuilder(nom != null ? nom : "");
        if (ville != null && !ville.isEmpty()) sb.append(" - ").append(ville);
        if (pays != null && !pays.isEmpty()) sb.append(", ").append(pays);
        return sb.toString();
    }

    @Override
    public String toString() {
        return nom != null ? nom : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ecole)) return false;
        Ecole ecole = (Ecole) o;
        return id != null && id.equals(ecole.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
