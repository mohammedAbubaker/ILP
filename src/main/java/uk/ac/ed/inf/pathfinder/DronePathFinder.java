package uk.ac.ed.inf.pathfinder;

import uk.ac.ed.inf.Context;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;

import java.util.*;

/**
 * This class is responsible for handling all DronePathFinding calculations.
 * Drone path processing can be explained in 3 steps.
 * <p>
 * Firstly, construct a {@link VisibilityGraph} by calling the default constructor.
 * </p>
 * <p>
 * Secondly, run A* on the {@link VisibilityGraph} to find the shortest path from {@link Context#getSrc() src} to {@link Context#getDest() dest}.
 * </p>
 * <p>
 * Thirdly, build a path using the constrained direction set with a greedy algorithm.
 * </p>
 * This path can be returned with {@link #getRoute()}
 */

public class DronePathFinder {
    private final VisibilityGraph visibilityGraph;
    private Context context;

    /** By passing in a context, the drone path retrieves the information it needs (such as {@link Context#getSrc() src}) with
     * Context getter functions.
     * (stage 1: 3 for finding path) construct a visibility graph by initialising it.
     * */
    public DronePathFinder(Context context) {
        this.visibilityGraph = new VisibilityGraph(context);
        this.context = context;
    }

    /** (stage 2: 3 for pathfinding construction)
     *  This function finds the shortest path between {@link Context#getSrc() src} and
     *  {@link Context#getDest() dest} in a visibility graph {@link VisibilityGraph}.
     *  It uses A* pathfinding, see this <a href="https://en.wikipedia.org/wiki/A*_search_algorithm"> wiki article</a>
     * @return LngLat[] an array of {@link LngLat} values that represent "waypoints". Waypoints can be seen as temporary
     * destinations that the drone follows to eventually reach {@link Context#getDest() dest}. Ready to be transformed into a
     * complete array with {@link #buildPath(LngLat, LngLat)}
     * */
    private LngLat[] pathfind() {
        // initialise data structures for A*
        HashMap<LngLat, Double> local = new HashMap<>();
        HashMap<LngLat, Double> global = new HashMap<>();
        // (smallest global cost is at the front of the priority queue)
        PriorityQueue<LngLat> open = new PriorityQueue<>
                (Comparator.comparingDouble(o -> global.getOrDefault(o, Double.MAX_VALUE)));
        HashMap<LngLat, LngLat> parent = new HashMap<>();
        HashSet<LngLat> closed = new HashSet<>();

        // add the source node to the corresponding data structures
        open.add(this.context.getSrc());
        local.put(this.context.getSrc(), 0.0);
        global.put(this.context.getSrc(), heuristic(this.context.getSrc(), this.context.getDest()));
        parent.put(this.context.getSrc(), null);

        // a* pathfinding algorithm
        while (!open.isEmpty() && !open.peek().equals(this.context.getDest())) {
            LngLat current_node = open.poll();
            closed.add(current_node);
            for (LngLat neighbour : visibilityGraph.edgeSet.get(current_node)) {
                if (!closed.contains(neighbour)) {
                    open.add(neighbour);
                }
                double lower = local.getOrDefault(current_node, Double.MAX_VALUE) + heuristic(current_node, neighbour);
                if (lower < local.getOrDefault(neighbour, Double.MAX_VALUE)) {
                    parent.put(neighbour, current_node);
                    local.put(neighbour, lower);
                    global.put(neighbour, local.get(neighbour) + heuristic(neighbour, this.context.getDest()));
                }
            }
        }
        return constructWaypoints(parent);
    }

    // heuristic function for A* is euclidean distance
    private static double heuristic(LngLat a, LngLat b) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        return lngLatHandler.distanceTo(a, b);
    }

    /**
     * This function works by setting the current node to {@link Context#getDest() dest}, then continously setting
     * current node to current node's parent until current node's parent is the {@link Context#getSrc() src} which
     * results in getting the shortest path.
     *
     * @param parent Constructed with {@link #pathfind()}. Calling parent.get(current_node) returns the parent of
     *               current_node.
     * @return A list of waypoints. Used to finalise the {@link #pathfind()} function.
     * */
    private LngLat[] constructWaypoints(HashMap<LngLat, LngLat> parent) {
        ArrayList<LngLat> waypoints = new ArrayList<>();
        LngLat current = this.context.getDest();
        if (parent.containsKey(current)) {
            while (!current.equals(this.context.getSrc())) {
                waypoints.add(0, current);
                current = parent.get(current);
            }
            waypoints.add(0, this.context.getSrc());
        }
        else {
            System.err.println("Error: Cannot find dest. Drone path is empty.");
        }
        return waypoints.toArray(new LngLat[0]);
    }

    /**
     * (stage 3: 3 for pathfinding construction) get the list of waypoints by calling the {@link #pathfind()} function.
     * Starting from {@link Context#getSrc() src}, store the incremental steps that the drone needs to take in order to be close
     * to (defined from {@link LngLatHandler#isCloseTo(LngLat, LngLat)}) the next waypoint. Eventually, the last
     * waypoint that the drone has to incrementally step towards is the destination itself, representing the complete
     * drone path from {@link Context#getSrc() src} to {@link Context#getDest() dest}. The drone path is then reversed and appended onto itself, to
     * present the drone pathing back to {@link Context#getSrc() src}.
     * @return The complete list of incremental drone moves to go from {@link Context#getSrc() src} to {@link Context#getDest() dest}.
     * */
    public ArrayList<LngLat> getRoute() {
        LngLat[] waypoints = pathfind();

        // sum the distance of the waypoints
        /*
        *   double sum_of_waypoints = 0.0;
            LngLatHandler lngLatHandler = new LngLatHandler();
            for (int i = 0; i < waypoints.length - 1; i++) {
                sum_of_waypoints += (lngLatHandler.distanceTo(waypoints[i], waypoints[i+1]));
            }
        * */


        ArrayList<LngLat> dronePath = new ArrayList<>();
        dronePath.add(waypoints[0]);
        for (int i = 1; i < waypoints.length; i++) {
            dronePath.addAll(List.of(buildPath(dronePath.get(dronePath.size() - 1), waypoints[i])));
        }

        // double total_path_length = dronePath.size() * SystemConstants.DRONE_MOVE_DISTANCE;

        // reverse path to get distance back to src
        for (int j = dronePath.size() - 1; j > 0; j--) {
            dronePath.add(dronePath.get(j));
        }
        // System.out.println(sum_of_waypoints / total_path_length * 100.0);

        // duplicate the last index to indiciate hover move
        dronePath.add(dronePath.get(dronePath.size() - 1));
        // calculate total path length
        return dronePath;
    }

    /**
     * This function, given a source waypoint wp_s and a destination waypoint wp_d will return the incremental steps
     * required to reach wp_d from wp_s with its legal set of 16 direction compass moves. It does this by incrementing
     * the current node until the current node is close to the destination node. The next angle is picked via a greedy
     * approach, where the angle from the 16 directions closest to the non-legal angle formed from wp_s to wp_d is selected.
     * @param wp_s The position where the drone currently is
     * @param wp_d The position where the drone wants to go
     * @return A list of drone positions which represent the incremental steps that the drone has to take in order to
     * go from wp_s to wp_d.
     * */
    private LngLat[] buildPath(LngLat wp_s, LngLat wp_d) {
        LngLat current_node = wp_s;
        LngLatHandler lngLatHandler = new LngLatHandler();
        ArrayList<LngLat> dronePath = new ArrayList<>();
        while (!lngLatHandler.isCloseTo(current_node, wp_d)) {
            double angle = 0.0;
            ArrayList<Double> angles = new ArrayList<>();
            while (angle < 360) {
                LngLat potential_neighbour = lngLatHandler.nextPosition(current_node, angle);
                if (!lngLatHandler.isInRegions(potential_neighbour, this.context.getNoFlyZones())) {
                    angles.add(angle);
                }
                angle += 22.5;
            }
            Double current_angle = getAngle(current_node, wp_d);
            angles.sort(Comparator.comparingDouble(o -> Math.abs(current_angle - o)));
            dronePath.add(current_node);
            // utilises greedy approach by grabbing the smallest angle
            current_node = lngLatHandler.nextPosition(current_node, angles.get(0));
            if (lngLatHandler.isCloseTo(current_node, wp_d)) {
                dronePath.add(current_node);
                return dronePath.toArray(new LngLat[0]);
            }
        }
        System.err.println("Error: could not build path! Program terminating...");
        return dronePath.toArray(new LngLat[0]);
    }

    // used for buildPath() comparisons
    // takes 0 degrees to mean east.
    public static double getAngle(LngLat p, LngLat q) {
        if (p == q) {
            return 999.0;
        }
        double angle = Math.toDegrees(Math.atan2(q.lat() - p.lat(), q.lng() - p.lng()));
        if (angle < 0.0) {
            return angle + 360.0;
        } else {
            return angle;
        }
    }
}