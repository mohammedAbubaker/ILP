package uk.ac.ed.inf.pathfinder;


import uk.ac.ed.inf.Context;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.awt.geom.Line2D;
import java.util.*;


/**
 * The visibility graph is responsible for computing the shortest path from a source node to an end node.
 * Its {@link #constructEdges()} method is O(n^3) time complexity, where n represents the sum of all nodes
 * (2 (source and dest) + centralArea vertices + * all noFlyZone vertices).
 * */
public class VisibilityGraph {
    ArrayList<LngLat> nodes;
    // adjacency list representation
    HashMap<LngLat, ArrayList<LngLat>> edgeSet;
    private Context context;

    /**
     * Context is passed in so that {@link #constructNodes()} and {@link #constructEdges()} can get the information
     * they need.
     * */
    public VisibilityGraph(Context context) {
        // fill nodes with source, centralArea and noFlyZone vertices
        this.context = context;
        constructNodes();
        constructEdges();
    }

    /**
     * This function is responsible for initialising a list of nodes then filling it with all the relevant polygonal
     * data. These are the nodes of the visibility graph.
     * */
    private void constructNodes() {
        this.nodes = new ArrayList<>();
        this.nodes.add(this.context.getSrc());
        this.nodes.add(this.context.getDest());
        this.nodes.addAll(List.of(this.context.getCentralArea().vertices()));
        for (NamedRegion zone: this.context.getNoFlyZones()) {
            this.nodes.addAll(List.of(zone.vertices()));
        }
    }

    /**
     * For every pair-wise combination of nodes, check if the line segment they form is not obstructed by any
     * {@link Context#getNoFlyZones() no-fly zone}. If not obstructed then add to the {@link #edgeSet},
     * otherwise do not add. O(n^3) time complexity where n is the number of {@link #nodes}.
     * */
    private void constructEdges() {
        this.edgeSet = new HashMap<>();
        // construct edges
        for (int i = 0; i < this.nodes.size() - 1; i ++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                // test for visibility with distinct pairwise combination of nodes
                if (hasVisibility(nodes.get(i), nodes.get(j))) {
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
    }

    /** O(n) time complexity where n refers to the number of {@link Context#getNoFlyZones() no-fly zone} vertices.
     * Utilises Java's {@link Line2D} library to calculate intersection cleanly. Checking whether
     * 2 segments intersect is a constant time procedure.
     * @return False if the line segment defined by p and q intersects with at least one
     * {@link Context#getNoFlyZones() no-fly zone} segment.
     * True if the line segment defined by p and q does not intersect with any
     * {@link Context#getNoFlyZones() no-fly zone} segment.
     * */
    private boolean hasVisibility(LngLat p, LngLat q) {
        for (NamedRegion zone : this.context.getNoFlyZones())  {
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
}