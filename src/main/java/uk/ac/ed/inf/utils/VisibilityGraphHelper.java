package uk.ac.ed.inf.utils;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class VisibilityGraphHelper {
    private static boolean onSegment(LngLat p, LngLat q, LngLat r) {
        if (q.lng() <= Math.max(p.lng(), r.lng()) && q.lng() >= Math.min(p.lng(), r.lng()) &&
            q.lat() <= Math.max(p.lat(), r.lat()) && q.lat() >= Math.min(p.lat(), r.lat())) {
            return true;
        }
        return false;
    }
    // 1 = clockwise, 2 = anticlockwise, 0 = collinear
    private static int orientation(LngLat p, LngLat q, LngLat r) {
        double val = (q.lat() - p.lat()) * (r.lng() - q.lng()) - (q.lng() - p.lng()) * (r.lat() - q.lat());
        if (val == 0.0) {
            return 0;
        }
        return (val > 0.0)? 1: 2;
    }
    /*
    Returns true if the line p1p2 intersects with the line q1q2
    */
    // finish implementing this algorihtm: https://www.science.smith.edu/~istreinu/Teaching/Courses/274/Spring98/Projects/Philip/fp/algVisibility.htm
    // TODO
    private static boolean lineIntersect(LngLat p1, LngLat p2, LngLat q1, LngLat q2, HashSet<LngLat> obs) {
        // check for any matches
        LngLat[] arr = {p1, p2, q1, q2};
        for (int i = 0; i  < arr.length; i++) {
            for (int j = 0; j < arr.length; j++) {
                if (arr[i] == arr[j] && i != j) {
                    if (obs.contains(p1) && obs.contains(p2) && obs.contains(q1) && obs.contains(q2)) {
                        return true;
                    }
                    return false;
                }
            }
        }
        int o1 = orientation(p1, p2, q1);
        int o2 = orientation(p1, p2, q2);
        int o3 = orientation(q1, q2, p1);
        int o4 = orientation(q1, q2, p2);
        // General case

        if (o1 != o2 && o3 != o4) {
            return true;
        }
        // Special case
        if (o1 == 0 && onSegment(p1, q1, p2)) return true;
        if (o2 == 0 && onSegment(p1, q2, p2)) return true;
        if (o3 == 0 && onSegment(q1, p1, q2)) return true;
        if (o4 == 0 && onSegment(q1, p2, q2)) return true;
        return false;
    }
    /*

    */
    public static boolean hasLineOfSight(LngLat p, LngLat q, NamedRegion[] noFlyZones, HashSet<LngLat> obs) {
        boolean lineOfSight = true;
        for (NamedRegion region: noFlyZones) {
            for (int i = 0; i < region.vertices().length - 1; i++) {
                if (lineIntersect(p, q, region.vertices()[i], region.vertices()[i+1], obs)) {
                    lineOfSight = false;
                    break;
                }
            }
        }
        return lineOfSight;
    }

    /*
    process of vigraph construction:
    construct test for visiblity with start node and every wall node
    construct test for visibility with dest node and every wall node
    shoot ray from src node to dest node
    vigraph completed
    * */


    // the index i denotes which wall we are looking at
    public static boolean visible(LngLat p, int i) {
        return false;
    }
}
