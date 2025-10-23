package application;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Alerte {
    private Long id;
    private Long stageId;
    private Long stagiaireId;
    private String nomStagiaire;
    private String typeStagiaire;
    private String nomEcole;
    private String pays;
    private LocalDate dateFinFormation;
    private String niveauUrgence; // "CRITIQUE", "IMPORTANT", "MOYEN", "INFO"
    private boolean lue;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Constructeurs
    public Alerte() {
    }
    
    public Alerte(Long stageId, Long stagiaireId, String nomStagiaire, LocalDate dateFinFormation) {
        this.stageId = stageId;
        this.stagiaireId = stagiaireId;
        this.nomStagiaire = nomStagiaire;
        this.dateFinFormation = dateFinFormation;
        this.lue = false;
        this.dateCreation = LocalDateTime.now();
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getStageId() {
        return stageId;
    }
    
    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }
    
    public Long getStagiaireId() {
        return stagiaireId;
    }
    
    public void setStagiaireId(Long stagiaireId) {
        this.stagiaireId = stagiaireId;
    }
    
    public String getNomStagiaire() {
        return nomStagiaire;
    }
    
    public void setNomStagiaire(String nomStagiaire) {
        this.nomStagiaire = nomStagiaire;
    }
    
    public String getTypeStagiaire() {
        return typeStagiaire;
    }
    
    public void setTypeStagiaire(String typeStagiaire) {
        this.typeStagiaire = typeStagiaire;
    }
    
    public String getNomEcole() {
        return nomEcole;
    }
    
    public void setNomEcole(String nomEcole) {
        this.nomEcole = nomEcole;
    }
    
    public String getPays() {
        return pays;
    }
    
    public void setPays(String pays) {
        this.pays = pays;
    }
    
    public LocalDate getDateFinFormation() {
        return dateFinFormation;
    }
    
    public void setDateFinFormation(LocalDate dateFinFormation) {
        this.dateFinFormation = dateFinFormation;
        this.niveauUrgence = calculerNiveauUrgence();
    }
    
    public String getNiveauUrgence() {
        return niveauUrgence;
    }
    
    public void setNiveauUrgence(String niveauUrgence) {
        this.niveauUrgence = niveauUrgence;
    }
    
    public boolean isLue() {
        return lue;
    }
    
    public void setLue(boolean lue) {
        this.lue = lue;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public LocalDateTime getDateModification() {
        return dateModification;
    }
    
    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }
    
    // Méthode pour calculer le niveau d'urgence
    private String calculerNiveauUrgence() {
        if (dateFinFormation == null) return "INFO";
        
        long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(), 
            dateFinFormation
        );
        
        if (joursRestants <= 14) {
            return "CRITIQUE"; // 2 semaines ou moins
        } else if (joursRestants <= 30) {
            return "IMPORTANT"; // 1 mois ou moins
        } else if (joursRestants <= 90) {
            return "MOYEN"; // 3 mois ou moins
        } else {
            return "INFO"; // Plus de 3 mois
        }
    }
    
    public long getJoursRestants() {
        if (dateFinFormation == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dateFinFormation);
    }
}