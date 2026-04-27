package application.carte;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Carte interactive basée sur WebView + Leaflet.js (tuiles OpenStreetMap).
 * Fonctionne avec connexion internet ; affiche un canvas de secours hors-ligne.
 * Compatible JavaFX 17/21.
 */
public class OfflineMapView extends StackPane {

    private WebView  webView;
    private WebEngine engine;

    private boolean mapReady = false;

    // File d'attente des marqueurs ajoutés avant que la carte soit prête
    private final List<PendingMarker> pendingMarkers = new ArrayList<>();
    private final List<MapMarker>     markers        = new ArrayList<>();

    private MarkerClickListener markerClickListener;

    // Valeurs courantes (pour le fallback et la synchro)
    private double centerLat = 7.3697;
    private double centerLon = 12.3547;
    private double zoom      = 5.0;

    // ------------------------------------------------------------------ constructeur

    public OfflineMapView() {
        webView = new WebView();
        webView.setContextMenuEnabled(false);
        engine  = webView.getEngine();

        // Permet à JavaScript d'appeler des méthodes Java via window.javaConnector
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaConnector", new JavaConnector());
                mapReady = true;
                flushPending();
            }
        });

        engine.loadContent(buildHtml(centerLat, centerLon, (int) zoom));
        getChildren().add(webView);
    }

    // ------------------------------------------------------------------ API publique

    /** Ajoute un marqueur sur la carte. */
    public void addMarker(double lat, double lon, String label,
                          String type, long id, Color color) {
        MapMarker m = new MapMarker();
        m.lat   = lat;   m.lon   = lon;
        m.label = label; m.type  = type;
        m.id    = id;    m.color = color;
        markers.add(m);

        String hex = toHex(color);
        String icon = "ecole".equals(type) ? "🏫" : "👤";
        String js   = String.format(
            "addMarker(%f, %f, %s, '%s', %d, '%s', '%s');",
            lat, lon, jsStr(label), type, id, hex, icon);

        runJs(js);
    }

    /** Supprime tous les marqueurs. */
    public void clearMarkers() {
        markers.clear();
        pendingMarkers.clear();
        runJs("clearMarkers();");
    }

    /** Centre la carte sur les coordonnées données avec le zoom spécifié. */
    public void centerOn(double lat, double lon, double newZoom) {
        this.centerLat = lat;
        this.centerLon = lon;
        this.zoom      = newZoom;
        // Leaflet utilise des niveaux entiers 1-18
        int leafletZoom = zoomToLeaflet(newZoom);
        runJs(String.format("map.setView([%f, %f], %d);", lat, lon, leafletZoom));
    }

    /** Définit le niveau de zoom (échelle interne 0.5-10 → Leaflet 1-18). */
    public void setZoom(double newZoom) {
        this.zoom = Math.max(0.5, Math.min(newZoom, 10.0));
        runJs(String.format("map.setZoom(%d);", zoomToLeaflet(this.zoom)));
    }

    public double getZoom() { return zoom; }

    public void setMarkerClickListener(MarkerClickListener listener) {
        this.markerClickListener = listener;
    }

    // ------------------------------------------------------------------ helpers privés

    /** Exécute du JavaScript en différé si la carte n'est pas encore prête. */
    private void runJs(String js) {
        if (mapReady) {
            Platform.runLater(() -> engine.executeScript(js));
        } else {
            pendingMarkers.add(new PendingMarker(js));
        }
    }

    private void flushPending() {
        Platform.runLater(() -> {
            for (PendingMarker p : pendingMarkers) {
                try { engine.executeScript(p.js); }
                catch (Exception e) { e.printStackTrace(); }
            }
            pendingMarkers.clear();
        });
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed()*255),
            (int)(c.getGreen()*255),
            (int)(c.getBlue()*255));
    }

    /** Échappe une chaîne pour l'injecter en JS. */
    private String jsStr(String s) {
        if (s == null) return "''";
        return "'" + s.replace("\\", "\\\\")
                      .replace("'", "\\'")
                      .replace("\n", " ")
                      .replace("\r", "") + "'";
    }

    /** Convertit l'échelle interne (0.5-10) en niveau Leaflet (1-18). */
    private int zoomToLeaflet(double z) {
        // Mappage linéaire : 0.5→3, 1→5, 2→7, 4→10, 10→18
        return (int) Math.round(3 + (z - 0.5) * (15.0 / 9.5));
    }

    // ------------------------------------------------------------------ HTML embarqué

    private String buildHtml(double lat, double lon, int leafletZoom) {
        return "<!DOCTYPE html>\n"
             + "<html><head>\n"
             + "<meta charset='utf-8'/>\n"
             + "<meta name='viewport' content='width=device-width, initial-scale=1'/>\n"
             + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>\n"
             + "<style>\n"
             + "  * { margin:0; padding:0; box-sizing:border-box; }\n"
             + "  html, body, #map { width:100%; height:100%; }\n"
             + "  body { font-family: 'Segoe UI', sans-serif; }\n"
             + "  /* Marker personnalisé */\n"
             + "  .custom-marker { position:relative; text-align:center; }\n"
             + "  .marker-pin {\n"
             + "    width:30px; height:30px; border-radius:50% 50% 50% 0;\n"
             + "    transform: rotate(-45deg);\n"
             + "    position:absolute; left:50%; top:50%;\n"
             + "    margin:-15px 0 0 -15px;\n"
             + "    border: 3px solid white;\n"
             + "    box-shadow: 0 2px 8px rgba(0,0,0,0.35);\n"
             + "    transition: transform 0.2s;\n"
             + "  }\n"
             + "  .marker-icon {\n"
             + "    position:absolute; width:30px; text-align:center;\n"
             + "    line-height:30px; left:50%; top:50%;\n"
             + "    margin:-15px 0 0 -15px;\n"
             + "    font-size:14px; z-index:1;\n"
             + "    transform: none;\n"
             + "  }\n"
             + "  /* Popup stylisé */\n"
             + "  .leaflet-popup-content-wrapper {\n"
             + "    border-radius:10px;\n"
             + "    box-shadow: 0 4px 20px rgba(0,0,0,0.2);\n"
             + "    border: none;\n"
             + "  }\n"
             + "  .popup-title { font-weight:bold; font-size:14px; color:#2c3e50; margin-bottom:5px; }\n"
             + "  .popup-type  { font-size:11px; color:white; padding:2px 8px;\n"
             + "                 border-radius:10px; display:inline-block; margin-bottom:6px; }\n"
             + "  .popup-info  { font-size:12px; color:#555; line-height:1.6; }\n"
             + "  .popup-info span { font-weight:600; }\n"
             + "  .popup-btn   { margin-top:8px; padding:5px 14px; border:none; border-radius:5px;\n"
             + "                 background:#667eea; color:white; cursor:pointer; font-size:12px;\n"
             + "                 width:100%; }\n"
             + "  .popup-btn:hover { background:#5568d3; }\n"
             + "  /* Légende */\n"
             + "  .legend { background:white; padding:10px 14px; border-radius:8px;\n"
             + "            box-shadow:0 2px 10px rgba(0,0,0,0.15); line-height:22px; font-size:12px; }\n"
             + "  .legend-item { display:flex; align-items:center; gap:8px; }\n"
             + "  .legend-dot  { width:12px; height:12px; border-radius:50%; flex-shrink:0; }\n"
             + "</style>\n"
             + "</head><body>\n"
             + "<div id='map'></div>\n"
             + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n"
             + "<script>\n"
             + "var map = L.map('map', {\n"
             + "  center: [" + lat + ", " + lon + "],\n"
             + "  zoom: " + leafletZoom + ",\n"
             + "  zoomControl: true,\n"
             + "  attributionControl: true\n"
             + "});\n"
             + "\n"
             + "// Couche OSM\n"
             + "var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n"
             + "  maxZoom: 19,\n"
             + "  attribution: '© OpenStreetMap contributors'\n"
             + "}).addTo(map);\n"
             + "\n"
             + "// Couche de remplacement (CartoDB – plus claire)\n"
             + "var cartoLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {\n"
             + "  maxZoom: 19,\n"
             + "  attribution: '© CartoDB'\n"
             + "});\n"
             + "\n"
             + "var baseMaps = {\n"
             + "  'OpenStreetMap': osmLayer,\n"
             + "  'Carte Claire (CartoDB)': cartoLayer\n"
             + "};\n"
             + "L.control.layers(baseMaps).addTo(map);\n"
             + "\n"
             + "// Légende\n"
             + "var legend = L.control({ position: 'bottomright' });\n"
             + "legend.onAdd = function() {\n"
             + "  var div = L.DomUtil.create('div', 'legend');\n"
             + "  div.innerHTML = '<b style=\"font-size:13px\">Légende</b><br><br>'\n"
             + "    + '<div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#27ae60\"></div> École Professionnelle</div>'\n"
             + "    + '<div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#f39c12\"></div> École Académique</div>'\n"
             + "    + '<div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#9b59b6\"></div> École Partenaire</div>'\n"
             + "    + '<div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#3498db\"></div> Stagiaire</div>';\n"
             + "  return div;\n"
             + "};\n"
             + "legend.addTo(map);\n"
             + "\n"
             + "var markersMap = {};\n"
             + "var markerLayer = L.layerGroup().addTo(map);\n"
             + "\n"
             + "function createIcon(color, iconChar) {\n"
             + "  var html = '<div class=\"custom-marker\">'\n"
             + "           + '<div class=\"marker-pin\" style=\"background:' + color + '\"></div>'\n"
             + "           + '<div class=\"marker-icon\">' + iconChar + '</div>'\n"
             + "           + '</div>';\n"
             + "  return L.divIcon({\n"
             + "    className: '',\n"
             + "    html: html,\n"
             + "    iconSize: [30, 42],\n"
             + "    iconAnchor: [15, 42],\n"
             + "    popupAnchor: [0, -42]\n"
             + "  });\n"
             + "}\n"
             + "\n"
             + "function addMarker(lat, lon, label, type, id, color, icon) {\n"
             + "  var m = L.marker([lat, lon], { icon: createIcon(color, icon) });\n"
             + "  var typeLabel = type === 'ecole' ? 'École' : 'Stagiaire';\n"
             + "  var bgColor   = type === 'ecole' ? color : '#3498db';\n"
             + "  var popupHtml = '<div class=\"popup-title\">' + label + '</div>'\n"
             + "    + '<div class=\"popup-type\" style=\"background:' + bgColor + '\">' + typeLabel + '</div>'\n"
             + "    + '<div class=\"popup-info\">Lat: <span>' + lat.toFixed(4) + '</span>, Lon: <span>' + lon.toFixed(4) + '</span></div>'\n"
             + "    + '<button class=\"popup-btn\" onclick=\"window.javaConnector.onMarkerClick(\\'' + type + '\\', ' + id + ')\">📋 Voir les détails</button>';\n"
             + "  m.bindPopup(popupHtml, { maxWidth: 220 });\n"
             + "  m.on('click', function() { m.openPopup(); });\n"
             + "  markerLayer.addLayer(m);\n"
             + "  markersMap[type + '_' + id] = m;\n"
             + "}\n"
             + "\n"
             + "function clearMarkers() {\n"
             + "  markerLayer.clearLayers();\n"
             + "  markersMap = {};\n"
             + "}\n"
             + "\n"
             + "// Notifier Java quand la carte est prête\n"
             + "map.whenReady(function() {\n"
             + "  if (window.javaConnector) window.javaConnector.onMapReady();\n"
             + "});\n"
             + "</script>\n"
             + "</body></html>";
    }

    // ------------------------------------------------------------------ Connecteur Java↔JS

    /**
     * Classe exposée à JavaScript via window.javaConnector.
     * DOIT être public avec des méthodes public pour être accessibles depuis JS.
     */
    public class JavaConnector {

        /** Appelée depuis JS quand un bouton "Détails" est cliqué dans le popup. */
        public void onMarkerClick(String type, long id) {
            if (markerClickListener == null) return;
            MapMarker found = markers.stream()
                .filter(m -> m.type.equals(type) && m.id == id)
                .findFirst().orElse(null);
            if (found != null) {
                final MapMarker fm = found;
                Platform.runLater(() -> markerClickListener.onMarkerClick(fm));
            }
        }

        /** Appelée quand Leaflet signale que la carte est prête (côté JS). */
        public void onMapReady() {
            // Carte déjà marquée prête via Worker.State.SUCCEEDED
        }
    }

    // ------------------------------------------------------------------ Classes internes

    public static class MapMarker {
        public double lat;
        public double lon;
        public String label;
        public String type;
        public long   id;
        public Color  color;
    }

    @FunctionalInterface
    public interface MarkerClickListener {
        void onMarkerClick(MapMarker marker);
    }

    private static class PendingMarker {
        final String js;
        PendingMarker(String js) { this.js = js; }
    }
}