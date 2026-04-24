package application.carte;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Carte interactive offline basée sur Canvas JavaFX
 * Ne nécessite pas de connexion Internet
 */
public class OfflineMapView extends Pane {
    
    private Canvas canvas;
    private GraphicsContext gc;
    
    // Coordonnées de la carte
    private double centerLat = 7.3697; // Cameroun
    private double centerLon = 12.3547;
    private double zoom = 1.0;
    
    // Limites géographiques affichées
    private double minLat = -90;
    private double maxLat = 90;
    private double minLon = -180;
    private double maxLon = 180;
    
    // Marqueurs
    private List<MapMarker> markers = new ArrayList<>();
    
    // Interaction souris
    private double lastMouseX;
    private double lastMouseY;
    private boolean isDragging = false;
    
    // Callback pour les clics sur marqueurs
    private MarkerClickListener markerClickListener;
    
    public OfflineMapView() {
        canvas = new Canvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());
        
        gc = canvas.getGraphicsContext2D();
        
        getChildren().add(canvas);
        
        // Redessiner quand la taille change
        canvas.widthProperty().addListener((obs, old, newVal) -> redraw());
        canvas.heightProperty().addListener((obs, old, newVal) -> redraw());
        
        setupInteractions();
        
        // Dessiner la carte initiale
        redraw();
    }
    
    private void setupInteractions() {
        // Zoom avec molette
        canvas.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            double factor = delta > 0 ? 1.1 : 0.9;
            zoom *= factor;
            zoom = Math.max(0.5, Math.min(zoom, 10.0));
            redraw();
        });
        
        // Drag pour déplacer
        canvas.setOnMousePressed((MouseEvent event) -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            isDragging = true;
        });
        
        canvas.setOnMouseDragged((MouseEvent event) -> {
            if (isDragging) {
                double dx = event.getX() - lastMouseX;
                double dy = event.getY() - lastMouseY;
                
                // Convertir en degrés
                double lonDelta = (dx / canvas.getWidth()) * (maxLon - minLon);
                double latDelta = (dy / canvas.getHeight()) * (maxLat - minLat);
                
                centerLon -= lonDelta;
                centerLat += latDelta;
                
                // Limiter aux bornes
                centerLat = Math.max(-90, Math.min(90, centerLat));
                centerLon = Math.max(-180, Math.min(180, centerLon));
                
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                
                redraw();
            }
        });
        
        canvas.setOnMouseReleased((MouseEvent event) -> {
            isDragging = false;
        });
        
        // Clic sur marqueur
        canvas.setOnMouseClicked((MouseEvent event) -> {
            if (event.getButton() == MouseButton.PRIMARY && !isDragging) {
                double x = event.getX();
                double y = event.getY();
                
                for (MapMarker marker : markers) {
                    double[] coords = latLonToPixel(marker.lat, marker.lon);
                    double distance = Math.sqrt(
                        Math.pow(x - coords[0], 2) + Math.pow(y - coords[1], 2)
                    );
                    
                    if (distance <= 10) {
                        if (markerClickListener != null) {
                            markerClickListener.onMarkerClick(marker);
                        }
                        break;
                    }
                }
            }
        });
    }
    
    public void redraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        
        if (width == 0 || height == 0) return;
        
        // Calculer les limites affichées
        double latRange = 180.0 / zoom;
        double lonRange = 360.0 / zoom;
        
        minLat = centerLat - latRange / 2;
        maxLat = centerLat + latRange / 2;
        minLon = centerLon - lonRange / 2;
        maxLon = centerLon + lonRange / 2;
        
        // Limiter aux bornes
        minLat = Math.max(-90, minLat);
        maxLat = Math.min(90, maxLat);
        minLon = Math.max(-180, minLon);
        maxLon = Math.min(180, maxLon);
        
        // Effacer
        gc.setFill(Color.web("#e8f4f8"));
        gc.fillRect(0, 0, width, height);
        
        // Dessiner la grille
        drawGrid();
        
        // Dessiner les continents (simplifié)
        drawContinents();
        
        // Dessiner les marqueurs
        drawMarkers();
        
        // Dessiner les informations
        drawInfo();
    }
    
    private void drawGrid() {
        gc.setStroke(Color.web("#d0d0d0"));
        gc.setLineWidth(0.5);
        
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        
        // Lignes de latitude
        for (int lat = -90; lat <= 90; lat += 15) {
            if (lat >= minLat && lat <= maxLat) {
                double y = latToY(lat);
                gc.strokeLine(0, y, width, y);
            }
        }
        
        // Lignes de longitude
        for (int lon = -180; lon <= 180; lon += 15) {
            if (lon >= minLon && lon <= maxLon) {
                double x = lonToX(lon);
                gc.strokeLine(x, 0, x, height);
            }
        }
    }
    
    private void drawContinents() {
        gc.setFill(Color.web("#c8e6c9"));
        gc.setStroke(Color.web("#81c784"));
        gc.setLineWidth(1);
        
        // Afrique (simplifié)
        drawContinentShape(new double[][] {
            {37.0, -5.0}, {37.0, 30.0}, {15.0, 51.0}, 
            {-35.0, 51.0}, {-35.0, 20.0}, {-10.0, -5.0}
        });
        
        // Europe (simplifié)
        drawContinentShape(new double[][] {
            {71.0, -10.0}, {71.0, 40.0}, {36.0, 40.0}, {36.0, -10.0}
        });
        
        // Asie (simplifié)
        drawContinentShape(new double[][] {
            {75.0, 40.0}, {75.0, 150.0}, {10.0, 150.0}, {10.0, 40.0}
        });
        
        // Amériques (simplifié)
        drawContinentShape(new double[][] {
            {70.0, -170.0}, {70.0, -50.0}, {-55.0, -50.0}, {-55.0, -170.0}
        });
    }
    
    private void drawContinentShape(double[][] points) {
        if (points.length < 3) return;
        
        double[] xPoints = new double[points.length];
        double[] yPoints = new double[points.length];
        
        for (int i = 0; i < points.length; i++) {
            double[] pixel = latLonToPixel(points[i][0], points[i][1]);
            xPoints[i] = pixel[0];
            yPoints[i] = pixel[1];
        }
        
        gc.fillPolygon(xPoints, yPoints, points.length);
        gc.strokePolygon(xPoints, yPoints, points.length);
    }
    
    private void drawMarkers() {
        for (MapMarker marker : markers) {
            if (marker.lat >= minLat && marker.lat <= maxLat &&
                marker.lon >= minLon && marker.lon <= maxLon) {
                
                double[] coords = latLonToPixel(marker.lat, marker.lon);
                double x = coords[0];
                double y = coords[1];
                
                // Ombre du marqueur
                gc.setFill(Color.rgb(0, 0, 0, 0.3));
                gc.fillOval(x - 9, y - 9, 18, 18);
                
                // Marqueur
                gc.setFill(marker.color);
                gc.fillOval(x - 8, y - 8, 16, 16);
                
                // Bordure blanche
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeOval(x - 8, y - 8, 16, 16);
                
                // Label
                if (zoom > 2.0) {
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Arial", 10));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(marker.label, x, y - 12);
                }
            }
        }
    }
    
    private void drawInfo() {
        double width = canvas.getWidth();
        
        // Informations de zoom et position
        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        gc.fillRect(10, 10, 200, 60);
        
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 11));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("Zoom: %.2fx", zoom), 20, 30);
        gc.fillText(String.format("Lat: %.4f°", centerLat), 20, 45);
        gc.fillText(String.format("Lon: %.4f°", centerLon), 20, 60);
        
        // Légende
        gc.fillRect(width - 210, 10, 200, 80);
        gc.fillText("Légende:", width - 200, 30);
        
        drawLegendItem(width - 200, 45, Color.web("#27ae60"), "Écoles Prof.");
        drawLegendItem(width - 200, 60, Color.web("#f39c12"), "Écoles Acad.");
        drawLegendItem(width - 200, 75, Color.web("#3498db"), "Stagiaires");
    }
    
    private void drawLegendItem(double x, double y, Color color, String label) {
        gc.setFill(color);
        gc.fillOval(x, y - 5, 10, 10);
        gc.setFill(Color.BLACK);
        gc.fillText(label, x + 15, y + 3);
    }
    
    // Conversion coordonnées géographiques <-> pixels
    private double[] latLonToPixel(double lat, double lon) {
        double x = lonToX(lon);
        double y = latToY(lat);
        return new double[]{x, y};
    }
    
    private double latToY(double lat) {
        double height = canvas.getHeight();
        return height - ((lat - minLat) / (maxLat - minLat)) * height;
    }
    
    private double lonToX(double lon) {
        double width = canvas.getWidth();
        return ((lon - minLon) / (maxLon - minLon)) * width;
    }
    
    // Gestion des marqueurs
    public void addMarker(double lat, double lon, String label, String type, long id, Color color) {
        MapMarker marker = new MapMarker();
        marker.lat = lat;
        marker.lon = lon;
        marker.label = label;
        marker.type = type;
        marker.id = id;
        marker.color = color;
        markers.add(marker);
        redraw();
    }
    
    public void clearMarkers() {
        markers.clear();
        redraw();
    }
    
    public void centerOn(double lat, double lon, double newZoom) {
        this.centerLat = lat;
        this.centerLon = lon;
        this.zoom = newZoom;
        redraw();
    }
    
    public void setMarkerClickListener(MarkerClickListener listener) {
        this.markerClickListener = listener;
    }
    
    public void setZoom(double newZoom) {
        this.zoom = Math.max(0.5, Math.min(newZoom, 10.0));
        redraw();
    }

    public double getZoom() { return zoom; }
    
    // Classes internes
    public static class MapMarker {
        public double lat;
        public double lon;
        public String label;
        public String type;
        public long id;
        public Color color;
    }
    
    @FunctionalInterface
    public interface MarkerClickListener {
        void onMarkerClick(MapMarker marker);
    }
}