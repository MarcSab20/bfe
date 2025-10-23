package application;

import java.time.LocalDate;
import java.util.List;

public class Ecole {
    private Long id;
    private String nom;
    private String type; // "Professionnelle", "Académique", "Partenaire"
    private String pays;
    private String ville;
    private String adresse;
    private Double latitude;
    private Double longitude;
    private String contact;
    private String email;
    private String telephone;
    private String siteWeb;
    private List<String> specialites;
    private boolean partenaire;
    private LocalDate datePartenariat;
    private String description;
    private boolean actif;
    
    // Constructeurs
    public Ecole() {
    }
    
    public Ecole(Long id, String nom, String type, String pays, String ville) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.pays = pays;
        this.ville = ville;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getPays() {
        return pays;
    }
    
    public void setPays(String pays) {
        this.pays = pays;
    }
    
    public String getVille() {
        return ville;
    }
    
    public void setVille(String ville) {
        this.ville = ville;
    }
    
    public String getAdresse() {
        return adresse;
    }
    
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getContact() {
        return contact;
    }
    
    public void setContact(String contact) {
        this.contact = contact;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    
    public String getSiteWeb() {
        return siteWeb;
    }
    
    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }
    
    public List<String> getSpecialites() {
        return specialites;
    }
    
    public void setSpecialites(List<String> specialites) {
        this.specialites = specialites;
    }
    
    public boolean isPartenaire() {
        return partenaire;
    }
    
    public void setPartenaire(boolean partenaire) {
        this.partenaire = partenaire;
    }
    
    public LocalDate getDatePartenariat() {
        return datePartenariat;
    }
    
    public void setDatePartenariat(LocalDate datePartenariat) {
        this.datePartenariat = datePartenariat;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
}