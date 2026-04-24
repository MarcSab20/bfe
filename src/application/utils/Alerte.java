package application.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Alerte {

    public enum Niveau { INFO, AVERTISSEMENT, URGENT }

    private Long id;
    private Long stageId;
    private Long stagiaireId;
    private String nomStagiaire;
    private String typeStagiaire;
    private String nomEcole;
    private String pays;
    private LocalDate dateFinFormation;
    private boolean lue;
    private LocalDate dateCreation;
    private Niveau niveau;

    public Alerte() {
        this.lue = false;
        this.dateCreation = LocalDate.now();
        this.niveau = Niveau.INFO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }

    public Long getStagiaireId() { return stagiaireId; }
    public void setStagiaireId(Long stagiaireId) { this.stagiaireId = stagiaireId; }

    public String getNomStagiaire() { return nomStagiaire; }
    public void setNomStagiaire(String nomStagiaire) { this.nomStagiaire = nomStagiaire; }

    public String getTypeStagiaire() { return typeStagiaire; }
    public void setTypeStagiaire(String typeStagiaire) { this.typeStagiaire = typeStagiaire; }

    public String getNomEcole() { return nomEcole; }
    public void setNomEcole(String nomEcole) { this.nomEcole = nomEcole; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public LocalDate getDateFinFormation() { return dateFinFormation; }
    public void setDateFinFormation(LocalDate dateFinFormation) {
        this.dateFinFormation = dateFinFormation;
        calculerNiveau();
    }

    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }

    private void calculerNiveau() {
        if (dateFinFormation == null) return;
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), dateFinFormation);
        if (jours <= 7) {
            this.niveau = Niveau.URGENT;
        } else if (jours <= 14) {
            this.niveau = Niveau.AVERTISSEMENT;
        } else {
            this.niveau = Niveau.INFO;
        }
    }

    public long getJoursRestants() {
        if (dateFinFormation == null) return 0;
        return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), dateFinFormation));
    }

    public String getMessageAlerte() {
        long jours = getJoursRestants();
        if (jours == 0) return nomStagiaire + " - fin de stage aujourd'hui !";
        if (jours == 1) return nomStagiaire + " - fin de stage demain";
        return nomStagiaire + " - fin de stage dans " + jours + " jours";
    }

    @Override
    public String toString() {
        return getMessageAlerte();
    }
}
