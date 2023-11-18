package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.geojson.Feature;
import uk.ac.ed.inf.geojson.GeoJson;
import uk.ac.ed.inf.geojson.Geometry;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.awt.geom.Line2D;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;

public class VisibilityGraph {
    ArrayList<LngLat> nodes;
    HashSet<String> edges;

    NamedRegion[] noFlyZones;

    NamedRegion centralArea;

    // Given two points,
    private boolean hasVisibility(LngLat p, LngLat q) {
        for (NamedRegion zone : this.noFlyZones)  {
            for (int i = 0; i < zone.vertices().length - 1; i++) {
                // if both points lie on the same polygon then return false
                if (Arrays.asList(zone.vertices()).contains(p) && Arrays.asList(zone.vertices()).contains(q)) {
                    return false;
                }
                LngLat r = zone.vertices()[i];
                LngLat s = zone.vertices()[i+1];
                Line2D pq = new Line2D.Double(p.lng(), p.lat(), q.lng(), q.lat());
                Line2D rs = new Line2D.Double(r.lng(), r.lat(), s.lng(), s.lat());
                if (pq.intersectsLine(rs)) {
                    // ignore intersections that are just p = (r or s) or q = (r or s)
                    boolean eqP = r.equals(p) || s.equals(p);
                    boolean eqQ = r.equals(q) || s.equals(q);
                    if (eqP || eqQ) return false;
                }
            }
        }
        return true;
    }
    public VisibilityGraph(AppService appService) {
        // fill nodes with source, centralArea and noFlyZone vertices
        this.noFlyZones = appService.getNoFlyZones();
        this.centralArea = appService.getCentralArea();
        this.nodes = new ArrayList<>();
        this.nodes.add(appService.getSrc());
        this.nodes.addAll(List.of(appService.getCentralArea().vertices()));
        for (NamedRegion zone: appService.getNoFlyZones()) {
            this.nodes.addAll(List.of(zone.vertices()));
        }
        // construct edges
        this.edges = new HashSet<>();
        for (int i = 0; i < this.nodes.size() - 1; i ++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                // test for visibility with distinct pairwise combination of nodes
                if (hasVisibility(nodes.get(i), nodes.get(j))) {
                    edges.add(i + " " + j);
                }
            }
        }
    }

    private static boolean checkEdgeEquality(String edge, int n) {
        int i = Integer.parseInt(edge.split(" ")[0]);
        int j = Integer.parseInt(edge.split(" ")[1]);
        return ((i == n) || (j ==n));
    }

    // update visibility graph with destination information
    public void updateViGraph(AppService appService, LngLat dest) {
        int n = this.nodes.size() - 1;
        if (appService.getDest() != null) {
            this.edges.removeIf(e -> checkEdgeEquality(e, n));
            this.nodes.remove(n);
        }
        this.nodes.add(dest);
        for (int i = 0; i < this.nodes.size() - 1; i++) {
            if (hasVisibility(this.nodes.get(i), dest)) {
                edges.add(i + " " + n);
            }
        }
    }

    void plotGraph() {
        // generate feature collection
        ArrayList<Feature> featureCollection = new ArrayList<>();
        // add central area polygon
        featureCollection.add(new Feature(new Geometry(this.centralArea.vertices(), "Polygon"), "centralArea"));
        // add noflyzone polygons
        for (NamedRegion zone: this.noFlyZones) {
            featureCollection.add(new Feature(new Geometry(zone.vertices(), "Polygon"), "noFlyZone"));
        }
        // add vigraph lines
        for (String edge: this.edges) {
            featureCollection.add(
                    new Feature(
                            new Geometry(new LngLat[]{this.nodes.get(Integer.parseInt(edge.split(" ")[0])), nodes.get(Integer.parseInt(edge.split(" ")[1]))}, "LineString"), "vigraphLines"));

        }
        // generate geojson object
        GeoJson geojson = new GeoJson(featureCollection.toArray(Feature[]::new));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            String json = objectMapper.writeValueAsString(geojson);
            FileOutputStream fileOutputStream = new FileOutputStream("all.json");
            fileOutputStream.write(json.getBytes()); }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
