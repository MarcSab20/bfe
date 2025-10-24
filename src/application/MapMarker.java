package application;

/**
 * Classe représentant un marqueur sur la carte
 */
public class MapMarker {
    private String id;
    private String type; // "ecole" ou "stagiaire"
    private String nom;
    private double lat;
    private double lon;
    private String pays;
    private String ville;
    
    // Propriétés spécifiques aux écoles
    private String typeEcole;
    private int nbStagiaires;
    
    // Propriétés spécifiques aux stagiaires
    private String specialite;
    private String ecoleNom;
    private String statut;
    private String langue;
    
    // Constructeur par défaut
    public MapMarker() {
    }
    
    // Getters et Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getNom() {
        return nom;
    }
    
    public void setNom(String nom) {
        this.nom = nom;
    }
    
    public double getLat() {
        return lat;
    }
    
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public double getLon() {
        return lon;
    }
    
    public void setLon(double lon) {
        this.lon = lon;
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
    
    public String getTypeEcole() {
        return typeEcole;
    }
    
    public void setTypeEcole(String typeEcole) {
        this.typeEcole = typeEcole;
    }
    
    public int getNbStagiaires() {
        return nbStagiaires;
    }
    
    public void setNbStagiaires(int nbStagiaires) {
        this.nbStagiaires = nbStagiaires;
    }
    
    public String getSpecialite() {
        return specialite;
    }
    
    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }
    
    public String getEcoleNom() {
        return ecoleNom;
    }
    
    public void setEcoleNom(String ecoleNom) {
        this.ecoleNom = ecoleNom;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public String getLangue() {
        return langue;
    }
    
    public void setLangue(String langue) {
        this.langue = langue;
    }
}