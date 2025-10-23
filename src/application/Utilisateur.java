package application;

import java.time.LocalDateTime;

public class Utilisateur {
    private Long id;
    private String nomUtilisateur;
    private String motDePasseHash;
    private String niveauAcces; // "Chef de Bureau" ou "Chargé d'Étude Assistant"
    private String nom;
    private String prenom;
    private String email;
    private LocalDateTime derniereConnexion;
    private boolean actif;
    
    // Constructeurs
    public Utilisateur() {
    }
    
    public Utilisateur(Long id, String nomUtilisateur, String niveauAcces) {
        this.id = id;
        this.nomUtilisateur = nomUtilisateur;
        this.niveauAcces = niveauAcces;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNomUtilisateur() {
        return nomUtilisateur;
    }
    
    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }
    
    public String getMotDePasseHash() {
        return motDePasseHash;
    }
    
    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }
    
    public String getNiveauAcces() {
        return niveauAcces;
    }
    
    public void setNiveauAcces(String niveauAcces) {
        this.niveauAcces = niveauAcces;
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDateTime getDerniereConnexion() {
        return derniereConnexion;
    }
    
    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }
    
    public boolean isActif() {
        return actif;
    }
    
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    
    public boolean isChefDeBureau() {
        return "Chef de Bureau".equals(niveauAcces);
    }
    
    public boolean isChargeEtudeAssistant() {
        return "Chargé d'Étude Assistant".equals(niveauAcces);
    }
}