package application;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Gestionnaire MongoDB pour le stockage des documents
 */
public class MongoDBManager {
    
    private static final String MONGO_URL = "mongodb://localhost:27017";
    private static final String DB_NAME = "gestion_formation_docs";
    
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static GridFSBucket gridFSBucket;
    
    /**
     * Initialise la connexion à MongoDB
     */
    public static void initialize() {
        try {
            // Créer le client MongoDB
            mongoClient = MongoClients.create(MONGO_URL);
            
            // Sélectionner la base de données
            database = mongoClient.getDatabase(DB_NAME);
            
            // Initialiser GridFS pour le stockage de fichiers
            gridFSBucket = GridFSBuckets.create(database);
            
            System.out.println("✓ Connexion MongoDB établie avec succès");
            
            // Créer les collections si elles n'existent pas
            createCollectionsIfNotExist();
            
        } catch (Exception e) {
            System.err.println("✗ Erreur de connexion MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Retourne la base de données MongoDB
     */
    public static MongoDatabase getDatabase() {
        if (database == null) {
            initialize();
        }
        return database;
    }
    
    /**
     * Retourne le bucket GridFS
     */
    public static GridFSBucket getGridFSBucket() {
        if (gridFSBucket == null) {
            initialize();
        }
        return gridFSBucket;
    }
    
    /**
     * Crée les collections nécessaires
     */
    private static void createCollectionsIfNotExist() {
        try {
            // Collection pour les métadonnées des documents de stage
            createCollectionIfNotExists("stage_documents");
            
            // Collection pour les logs
            createCollectionIfNotExists("logs");
            
            System.out.println("✓ Collections MongoDB créées ou déjà existantes");
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la création des collections: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crée une collection si elle n'existe pas
     */
    private static void createCollectionIfNotExists(String collectionName) {
        boolean exists = false;
        for (String name : database.listCollectionNames()) {
            if (name.equals(collectionName)) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            database.createCollection(collectionName);
        }
    }
    
    /**
     * Sauvegarde un document dans MongoDB (GridFS)
     * 
     * @param file Le fichier à sauvegarder
     * @param stageId L'ID du stage associé
     * @param stagiaireId L'ID du stagiaire
     * @param uploadedBy L'utilisateur qui upload
     * @return L'ID du document dans MongoDB
     */
    public static String saveDocument(File file, String stageId, String stagiaireId, String uploadedBy) {
        try (InputStream streamToUploadFrom = new FileInputStream(file)) {
            
            // Créer les métadonnées
            Document metadata = new Document()
                .append("stageId", stageId)
                .append("stagiaireId", stagiaireId)
                .append("uploadedBy", uploadedBy)
                .append("uploadDate", new java.util.Date())
                .append("originalFilename", file.getName())
                .append("fileSize", file.length())
                .append("contentType", getContentType(file.getName()));
            
            // Upload le fichier dans GridFS
            ObjectId fileId = gridFSBucket.uploadFromStream(
                file.getName(),
                streamToUploadFrom,
                new com.mongodb.client.gridfs.model.GridFSUploadOptions().metadata(metadata)
            );
            
            // Sauvegarder les métadonnées dans la collection
            MongoCollection<Document> collection = database.getCollection("stage_documents");
            Document doc = new Document()
                .append("_id", fileId.toHexString())
                .append("stageId", stageId)
                .append("stagiaireId", stagiaireId)
                .append("filename", file.getName())
                .append("uploadedBy", uploadedBy)
                .append("uploadDate", new java.util.Date())
                .append("fileSize", file.length());
            collection.insertOne(doc);
            
            System.out.println("✓ Document sauvegardé: " + fileId.toHexString());
            return fileId.toHexString();
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la sauvegarde du document: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Récupère un document depuis MongoDB
     * 
     * @param documentId L'ID du document
     * @param outputPath Le chemin où sauvegarder le fichier
     * @return true si le téléchargement est réussi
     */
    public static boolean retrieveDocument(String documentId, String outputPath) {
        try {
            ObjectId fileId = new ObjectId(documentId);
            
            // Télécharger depuis GridFS
            try (OutputStream outputStream = new FileOutputStream(outputPath)) {
                gridFSBucket.downloadToStream(fileId, outputStream);
            }
            
            System.out.println("✓ Document récupéré: " + documentId);
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la récupération du document: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Supprime un document de MongoDB
     * 
     * @param documentId L'ID du document à supprimer
     * @return true si la suppression est réussie
     */
    public static boolean deleteDocument(String documentId) {
        try {
            ObjectId fileId = new ObjectId(documentId);
            
            // Supprimer de GridFS
            gridFSBucket.delete(fileId);
            
            // Supprimer les métadonnées
            MongoCollection<Document> collection = database.getCollection("stage_documents");
            collection.deleteOne(new Document("_id", documentId));
            
            System.out.println("✓ Document supprimé: " + documentId);
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la suppression du document: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Récupère les métadonnées d'un document depuis la collection
     * (PAS depuis GridFS - utiliser getGridFSFileInfo() pour ça)
     * 
     * @param documentId L'ID du document
     * @return Les métadonnées sous forme de Document, ou null si non trouvé
     */
    public static Document getDocumentMetadata(String documentId) {
        try {
            MongoCollection<Document> collection = database.getCollection("stage_documents");
            Document doc = collection.find(new Document("_id", documentId)).first();
            return doc;  // Retourne Document, PAS GridFSFile
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la récupération des métadonnées: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Récupère les informations complètes d'un fichier GridFS
     * 
     * @param documentId L'ID du document
     * @return Les informations du fichier GridFS
     */
    public static com.mongodb.client.gridfs.model.GridFSFile getGridFSFileInfo(String documentId) {
        try {
            ObjectId fileId = new ObjectId(documentId);
            
            // Rechercher le fichier dans GridFS
            com.mongodb.client.gridfs.model.GridFSFile gridFSFile = gridFSBucket
                .find(new Document("_id", fileId))
                .first();
            
            return gridFSFile;
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la récupération du fichier GridFS: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Log un événement dans MongoDB
     * 
     * @param userId ID de l'utilisateur
     * @param action Action effectuée
     * @param entityType Type d'entité
     * @param entityId ID de l'entité
     * @param details Détails supplémentaires
     */
    public static void logEvent(String userId, String action, String entityType, String entityId, String details) {
        try {
            MongoCollection<Document> collection = database.getCollection("logs");
            
            Document logEntry = new Document()
                .append("userId", userId)
                .append("action", action)
                .append("entityType", entityType)
                .append("entityId", entityId)
                .append("details", details)
                .append("timestamp", new java.util.Date())
                .append("ipAddress", getLocalIPAddress());
            
            collection.insertOne(logEntry);
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors du logging: " + e.getMessage());
        }
    }
    
    /**
     * Détermine le type de contenu d'un fichier
     */
    private static String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            default: return "application/octet-stream";
        }
    }
    
    /**
     * Récupère l'adresse IP locale
     */
    private static String getLocalIPAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Ferme la connexion MongoDB
     */
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("✓ Connexion MongoDB fermée");
        }
    }
}