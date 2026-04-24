package application.stages;

import application.ecoles.Ecole;
import application.stagiaires.Stagiaire;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StageFormation {

    private Long id;
    private Stagiaire stagiaire;
    private Ecole ecole;
    private String type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String specialite;
    private String encadrant;
    private String tuteur;
    private String objectifs;
    private String description;
    private String statut;
    private String documentId;
    private String documentNom;
    private String remarques;
    private LocalDate dateCreation;

    public StageFormation() {
        this.statut = "En cours";
        this.dateCreation = LocalDate.now();
    }

    public StageFormation(Stagiaire stagiaire, Ecole ecole, String type,
                          LocalDate dateDebut, LocalDate dateFin) {
        this();
        this.stagiaire = stagiaire;
        this.ecole = ecole;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Stagiaire getStagiaire() { return stagiaire; }
    public void setStagiaire(Stagiaire stagiaire) { this.stagiaire = stagiaire; }

    public Ecole getEcole() { return ecole; }
    public void setEcole(Ecole ecole) { this.ecole = ecole; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getEncadrant() { return encadrant; }
    public void setEncadrant(String encadrant) { this.encadrant = encadrant; }

    public String getTuteur() { return tuteur; }
    public void setTuteur(String tuteur) { this.tuteur = tuteur; }

    public String getObjectifs() { return objectifs; }
    public void setObjectifs(String objectifs) { this.objectifs = objectifs; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentNom() { return documentNom; }
    public void setDocumentNom(String documentNom) { this.documentNom = documentNom; }

    public String getRemarques() { return remarques; }
    public void setRemarques(String remarques) { this.remarques = remarques; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public long getDureeEnJours() {
        if (dateDebut == null || dateFin == null) return 0;
        return ChronoUnit.DAYS.between(dateDebut, dateFin);
    }

    public long getDureeEnMois() {
        if (dateDebut == null || dateFin == null) return 0;
        return ChronoUnit.MONTHS.between(dateDebut, dateFin);
    }

    public long getJoursRestants() {
        if (dateFin == null) return 0;
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), dateFin);
        return Math.max(0, jours);
    }

    public boolean isEnCours() {
        return "En cours".equals(statut);
    }

    public boolean isTermine() {
        return "Terminé".equals(statut);
    }

    public boolean isUrgent(int seuilJours) {
        return isEnCours() && getJoursRestants() <= seuilJours;
    }

    @Override
    public String toString() {
        String nom = stagiaire != null ? stagiaire.getNomComplet() : "?";
        String ecoleNom = ecole != null ? ecole.getNom() : "?";
        return nom + " @ " + ecoleNom + " (" + statut + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StageFormation)) return false;
        StageFormation s = (StageFormation) o;
        return id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
