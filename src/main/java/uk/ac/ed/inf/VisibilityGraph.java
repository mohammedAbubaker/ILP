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
import java.util.*;


public class VisibilityGraph {
    ArrayList<LngLat> nodes;
    HashSet<String> edges;

    NamedRegion[] noFlyZones;

    NamedRegion centralArea;
    HashMap<LngLat, ArrayList<LngLat>> edgeSet;

    // Given two points,
    private boolean hasVisibility(LngLat p, LngLat q) {
        for (NamedRegion zone : this.noFlyZones)  {
            for (int i = 0; i < zone.vertices().length - 1; i++) {
                // if both points lie on the same polygon then return false
                if (List.of(zone.vertices()).contains(p) && (List.of(zone.vertices()).contains(q))) {
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
                    if (!(eqP || eqQ)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    public VisibilityGraph(DronePathFinder dronePathFinder) {
        // fill nodes with source, centralArea and noFlyZone vertices
        this.noFlyZones = dronePathFinder.getNoFlyZones();
        this.centralArea = dronePathFinder.getCentralArea();
        this.nodes = new ArrayList<>();
        this.nodes.add(dronePathFinder.getSrc());
        this.nodes.add(dronePathFinder.getDest());
        this.edgeSet = new HashMap<>();
        this.nodes.addAll(List.of(dronePathFinder.getCentralArea().vertices()));
        for (NamedRegion zone: dronePathFinder.getNoFlyZones()) {
            this.nodes.addAll(List.of(zone.vertices()));
        }
        // construct edges
        this.edges = new HashSet<>();
        for (int i = 0; i < this.nodes.size() - 1; i ++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                // test for visibility with distinct pairwise combination of nodes
                if (hasVisibility(nodes.get(i), nodes.get(j))) {
                    edges.add(i + " " + j);
                    if (!edgeSet.containsKey(nodes.get(i))) {
                        edgeSet.put(nodes.get(i), new ArrayList<>());
                    }
                    if (!edgeSet.containsKey(nodes.get(j))) {
                        edgeSet.put(nodes.get(j), new ArrayList<>());
                    }
                    edgeSet.get(nodes.get(i)).add(nodes.get(j));
                    edgeSet.get(nodes.get(j)).add(nodes.get(i));
                }
            }
        }
        plotGraph();
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
