package application;

import java.time.LocalDate;

public class Stagiaire {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String nationalite;
    private String sexe;
    private String email;
    private String telephone;
    private String adresse;
    private String specialite;
    private String langue; // Langue principale du stage
    private String niveauEtude;
    private String diplome;
    private String typeFormation; // "Formation Initiale", "Formation Continue"
    private boolean actif;
    private LocalDate dateInscription;
    private String photo; // Chemin vers la photo
    private String remarques;
    
    // Constructeurs
    public Stagiaire() {
    }
    
    public Stagiaire(Long id, String matricule, String nom, String prenom) {
        this.id = id;
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMatricule() {
        return matricule;
    }
    
    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public String getPrenom() {
        return prenom;
    }
    
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    
    public LocalDate getDateNaissance() {
        return dateNaissance;
    }
    
    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }
    
    public String getLieuNaissance() {
        return lieuNaissance;
    }
    
    public void setLieuNaissance(String lieuNaissance) {
        this.lieuNaissance = lieuNaissance;
    }
    
    public String getNationalite() {
        return nationalite;
    }
    
    public void setNationalite(String nationalite) {
        this.nationalite = nationalite;
    }
    
    public String getSexe() {
        return sexe;
    }
    
    public void setSexe(String sexe) {
        this.sexe = sexe;
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
    
    public String getAdresse() {
        return adresse;
    }
    
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    
    public String getSpecialite() {
        return specialite;
    }
    
    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }
    
    public String getLangue() {
        return langue;
    }
    
    public void setLangue(String langue) {
        this.langue = langue;
    }
    
    public String getNiveauEtude() {
        return niveauEtude;
    }
    
    public void setNiveauEtude(String niveauEtude) {
        this.niveauEtude = niveauEtude;
    }
    
    public String getDiplome() {
        return diplome;
    }
    
    public void setDiplome(String diplome) {
        this.diplome = diplome;
    }
    
    public String getTypeFormation() {
        return typeFormation;
    }
    
    public void setTypeFormation(String typeFormation) {
        this.typeFormation = typeFormation;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    
    public LocalDate getDateInscription() {
        return dateInscription;
    }
    
    public void setDateInscription(LocalDate dateInscription) {
        this.dateInscription = dateInscription;
    }
    
    public String getPhoto() {
        return photo;
    }
    
    public void setPhoto(String photo) {
        this.photo = photo;
    }
    
    public String getRemarques() {
        return remarques;
    }
    
    public void setRemarques(String remarques) {
        this.remarques = remarques;
    }
    
    public String getNomComplet() {
        return nom + " " + prenom;
    }
}