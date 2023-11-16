package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.geojson.Feature;
import uk.ac.ed.inf.geojson.GeoJson;
import uk.ac.ed.inf.geojson.Geometry;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.utils.VisibilityGraphHelper;

import java.io.FileOutputStream;
import java.util.*;

import static uk.ac.ed.inf.utils.VisibilityGraphHelper.hasLineOfSight;

public class DronePathFinder {
    private LngLat src;
    private LngLat dest;
    private NamedRegion centralArea;
    private NamedRegion[] noFlyZones;
    public DronePathFinder() {}

    private void plotGraph(ArrayList<LngLat> nodes, HashSet<String> edges) {
        // generate feature collection
        ArrayList<Feature> featureCollection = new ArrayList<>();
        // add central area polygon
        featureCollection.add(new Feature(new Geometry(this.centralArea.vertices(), "Polygon"), "centralArea"));
        // add noflyzone polygons
        for (NamedRegion zone: this.noFlyZones) {
            featureCollection.add(new Feature(new Geometry(zone.vertices(), "Polygon"), "noFlyZone"));
        }
        // add vigraph lines
        for (String edge: edges) {
            int index1 = Integer.parseInt(edge.split(" ")[0]);
            int index2 = Integer.parseInt(edge.split(" ")[1]);
            featureCollection.add(new Feature(new Geometry(new LngLat[]{nodes.get(index1), nodes.get(index2)}, "LineString"), "vigraphLines"));
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


    /*
    Responsible for serializing a GeoJson object to create a map visualising the drone's path.
    */
    private void plotPath() {}
    /*
    Checks if point 1 has clear visibility with point 2, meaning if I shoot a ray from point 1 to point 2, does it
    intersect with any geometries (i.e. noFlyZones)?
    */
    private void constructVisibilityGraph() {
        // initialise node set
        ArrayList<LngLat> nodes = new ArrayList<>();
        nodes.add(this.src);
        nodes.add(this.dest);
        nodes.addAll(List.of(this.centralArea.vertices()));
        for (NamedRegion region: this.noFlyZones) {
            nodes.addAll(List.of(region.vertices()));
        }
        // initialise obstacle set
        HashSet<LngLat> obs = new HashSet<>();
        for (NamedRegion zone: noFlyZones) {
            for (LngLat vertex: zone.vertices()) {
                obs.add(vertex);
            }
        }
        // initialise edge set
        HashSet<String> edges = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                // if pair already considered then skip
                if (edges.contains(j + " " + i)) {
                    continue;
                }
                if (i == j) {
                    continue;
                }
                // check if nodes[i] has clear line of sight of nodes[j]
                if (hasLineOfSight(nodes.get(i), nodes.get(j), this.noFlyZones, obs)) {
                    edges.add(i + " " + j);
                }
            }
        }
        plotGraph(nodes, edges);
    }

    public void pathfind() {
        constructVisibilityGraph();
    }

    // ------------------------------------------------------------------
    // Setters

    public void setCentralArea(NamedRegion centralArea) {
        this.centralArea = centralArea;
    }
    public void setSrc(LngLat src) {
        this.src = src;
    }
    public void setDest(LngLat dest) {
        this.dest = dest;
    }
    public void setNoFlyZones(NamedRegion[] noFlyZones) {
        this.noFlyZones = noFlyZones;
    }
}
