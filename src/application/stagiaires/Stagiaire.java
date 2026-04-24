package application.stagiaires;

import java.time.LocalDate;
import java.time.Period;

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
    private String langue;
    private String niveauEtude;
    private String diplome;
    private String typeFormation;
    private boolean actif = true;
    private LocalDate dateInscription;
    private String photo;
    private String remarques;

    public Stagiaire() {}

    public Stagiaire(String matricule, String nom, String prenom) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.actif = true;
        this.dateInscription = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getLieuNaissance() { return lieuNaissance; }
    public void setLieuNaissance(String lieuNaissance) { this.lieuNaissance = lieuNaissance; }

    public String getNationalite() { return nationalite; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }

    public String getNiveauEtude() { return niveauEtude; }
    public void setNiveauEtude(String niveauEtude) { this.niveauEtude = niveauEtude; }

    public String getDiplome() { return diplome; }
    public void setDiplome(String diplome) { this.diplome = diplome; }

    public String getTypeFormation() { return typeFormation; }
    public void setTypeFormation(String typeFormation) { this.typeFormation = typeFormation; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getRemarques() { return remarques; }
    public void setRemarques(String remarques) { this.remarques = remarques; }

    public String getNomComplet() {
        String n = nom != null ? nom : "";
        String p = prenom != null ? prenom : "";
        return (n + " " + p).trim();
    }

    public int getAge() {
        if (dateNaissance == null) return 0;
        return Period.between(dateNaissance, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return getNomComplet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stagiaire)) return false;
        Stagiaire s = (Stagiaire) o;
        return id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
