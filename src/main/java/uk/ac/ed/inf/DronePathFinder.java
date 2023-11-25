package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.geojson.Feature;
import uk.ac.ed.inf.geojson.GeoJson;
import uk.ac.ed.inf.geojson.Geometry;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.*;

public class DronePathFinder {
    LngLat src;
    LngLat dest;
    NamedRegion[] noFlyZones;
    NamedRegion centralArea;
    VisibilityGraph visibilityGraph;
    public DronePathFinder(AppService appService) {
        this.src = appService.getSrc();
        this.dest = appService.getDest();
        this.noFlyZones = appService.getNoFlyZones();
        this.centralArea = appService.getCentralArea();
        this.visibilityGraph = new VisibilityGraph(this);
    }


    private double heuristic(LngLat a, LngLat b) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        return lngLatHandler.distanceTo(a, b);
    }
    // calculates shortest path
    public LngLat[] pathfind() {
        HashMap<LngLat, Double> local = new HashMap<>();
        HashMap<LngLat, Double> global = new HashMap<>();
        PriorityQueue<LngLat> open = new PriorityQueue<>(Comparator.comparingDouble(o -> global.getOrDefault(o, Double.MAX_VALUE)));
        HashMap<LngLat, LngLat> parent = new HashMap<>();
        HashSet<LngLat> closed = new HashSet<>();
        open.add(this.src);
        local.put(this.src, 0.0);
        global.put(this.src, heuristic(this.src, this.dest));
        parent.put(this.src, null);
        LngLat last_key = null;
        while (!open.isEmpty() && !open.peek().equals(dest)) {
            LngLat current_node = open.poll();
            closed.add(current_node);
            for (LngLat neighbour: visibilityGraph.edgeSet.get(current_node)) {
                if (!closed.contains(neighbour)) {
                    open.add(neighbour);
                }
                double lower = local.getOrDefault(current_node, Double.MAX_VALUE) + heuristic(current_node, neighbour);
                if (lower < local.getOrDefault(neighbour, Double.MAX_VALUE)) {
                    parent.put(neighbour, current_node);
                    local.put(neighbour, lower);
                    global.put(neighbour, local.get(neighbour) + heuristic(neighbour, dest));
                }
            }
            last_key = current_node;
        }
        parent.put(dest, last_key);
        Stack<LngLat> waypoints = new Stack<>();
        LngLat current = this.dest;
        while (!current.equals(this.src)) {
            waypoints.add(0, current);
            current = parent.get(current);
        }
        waypoints.add(0, this.src);
        return (waypoints.toArray(new LngLat[0]));
    }

    private static double getAngle(LngLat p, LngLat q) {
        double angle = Math.toDegrees(Math.atan2(q.lat() - p.lat(), q.lng() - p.lng()));
        if (angle < 0.0) {
            return angle + 360.0;
        }
        else {
            return angle;
        }
    }

    public LngLat[] buildPath(LngLat src, LngLat dest) {
        LngLat current_node = src;
        LngLatHandler lngLatHandler = new LngLatHandler();
        ArrayList<LngLat> dronePath = new ArrayList<>();
        while (!lngLatHandler.isCloseTo(current_node, dest)) {
            double angle = 0.0;
            ArrayList<Double> angles= new ArrayList<>();
            while (angle < 360) {
                LngLat potential_neighbour = lngLatHandler.nextPosition(current_node, angle);
                if (!lngLatHandler.isInRegions(potential_neighbour, this.noFlyZones)) {
                    angles.add(angle);
                }
                angle += 22.5;
            }
            Double current_angle = getAngle(current_node, dest);
            angles.sort(Comparator.comparingDouble(o -> Math.abs(current_angle - o)));
            dronePath.add(current_node);
            current_node = lngLatHandler.nextPosition(current_node, angles.get(0));
            if (lngLatHandler.isCloseTo(current_node, dest)) {
                break;
            }
        }
        return dronePath.toArray(new LngLat[0]);
    }

    public LngLat[] getRoute() {
        LngLat[] waypoints = pathfind();
        LngLat start = waypoints[0];
        LngLat wp = waypoints[1];
        ArrayList<LngLat> dronePath = new ArrayList<>(List.of(buildPath(start, wp)));
        for (int i = 2; i < waypoints.length; i++)  {
            dronePath.addAll(List.of(buildPath(dronePath.get(dronePath.size() - 1), waypoints[i])));
        }
        LngLat[] dronepath = dronePath.toArray(new LngLat[0]);
        return dronepath;
    }


    // public LngLat buildPath() {}


//    private LngLat[] pathfind()A {
//
//    }
//
//    public LngLat[] getRoute() {
//
//    }

    // Getters
    public LngLat getSrc() {
        return this.src;
    }

    public LngLat getDest() {
        return this.dest;
    }

    public NamedRegion[] getNoFlyZones() {
        return this.noFlyZones;
    }

    public NamedRegion getCentralArea() {
        return this.centralArea;
    }
}
