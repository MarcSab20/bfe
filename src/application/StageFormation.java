package application;

import java.time.LocalDate;

public class StageFormation {
    private Long id;
    private Stagiaire stagiaire;
    private Ecole ecole;
    private String type; // "Formation Initiale", "Formation Continue", "Stage Professionnel"
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String specialite;
    private String encadrant; // Nom de l'encadrant à l'école
    private String tuteur; // Tuteur de l'organisme d'envoi
    private String objectifs;
    private String description;
    private String statut; // "En cours", "Terminé", "Annulé", "Suspendu"
    private String documentId; // ID du document dans MongoDB
    private String documentNom; // Nom du document responsable de la mise en stage
    private String remarques;
    private LocalDate dateCreation;
    
    // Constructeurs
    public StageFormation() {
    }
    
    public StageFormation(Long id, Stagiaire stagiaire, Ecole ecole, LocalDate dateDebut, LocalDate dateFin) {
        this.id = id;
        this.stagiaire = stagiaire;
        this.ecole = ecole;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Stagiaire getStagiaire() {
        return stagiaire;
    }
    
    public void setStagiaire(Stagiaire stagiaire) {
        this.stagiaire = stagiaire;
    }
    
    public Ecole getEcole() {
        return ecole;
    }
    
    public void setEcole(Ecole ecole) {
        this.ecole = ecole;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public LocalDate getDateDebut() {
        return dateDebut;
    }
    
    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }
    
    public LocalDate getDateFin() {
        return dateFin;
    }
    
    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }
    
    public String getSpecialite() {
        return specialite;
    }
    
    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }
    
    public String getEncadrant() {
        return encadrant;
    }
    
    public void setEncadrant(String encadrant) {
        this.encadrant = encadrant;
    }
    
    public String getTuteur() {
        return tuteur;
    }
    
    public void setTuteur(String tuteur) {
        this.tuteur = tuteur;
    }
    
    public String getObjectifs() {
        return objectifs;
    }
    
    public void setObjectifs(String objectifs) {
        this.objectifs = objectifs;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getDocumentNom() {
        return documentNom;
    }
    
    public void setDocumentNom(String documentNom) {
        this.documentNom = documentNom;
    }
    
    public String getRemarques() {
        return remarques;
    }
    
    public void setRemarques(String remarques) {
        this.remarques = remarques;
    }
    
    public LocalDate getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public boolean isActif() {
        return "En cours".equals(statut);
    }
    
    public long getDureeEnJours() {
        if (dateDebut != null && dateFin != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
        }
        return 0;
    }

}