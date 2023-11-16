package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

import java.util.ArrayList;

import static java.lang.Math.sqrt;
import static java.lang.Math.pow;
import static java.lang.Math.toRadians;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.PI;

public class LngLatHandler implements LngLatHandling {
    @Override
    // Euclidean distance formula
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        return sqrt(pow((startPosition.lat() - endPosition.lat()), 2)  +
                pow((startPosition.lng() - endPosition.lng()), 2));
    }

    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) <= SystemConstants.DRONE_IS_CLOSE_DISTANCE;
    }

    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        /*
        Associate each vertex to form exactly one edge with its
        neighbour vertex, with the last vertex connected to the first.
        */
        ArrayList<LngLat[]> regionEdges = regionEdgesBuilder(region);
        /*
        then check if point lies within edges by
        using ray-casting algorithm and even-odd rule.
        treat lng values as y and lat values as x.
        https://en.wikipedia.org/wiki/Even–odd_rule
        */
        /*
        implementation not fully robust on edge cases
        */
        int sum = 0;
        for (LngLat[] tuple: regionEdges) {
            double x1 = tuple[0].lat();
            double x2 = tuple[1].lat();
            double y1 = tuple[0].lng();
            double y2 = tuple[1].lng();
            boolean a = (position.lng() < y1) != (position.lng() < y2);
            boolean b = position.lat() < (x1 + ((position.lng() - y1) / (y2 - y1)) * (x2-x1));
            if (a & b) {
                sum += 1;
            }
        }
        // even-odd rule applied here
        return (sum%2 == 1);
    }

    // returns true if point is in all regions
    public boolean isInRegions(LngLat point, NamedRegion[] regions) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        boolean inRegion = false;
        for (NamedRegion region: regions) {
            if (lngLatHandler.isInRegion(point, region)) {
                inRegion = true;
                break;
            }
        }
        return inRegion;
    }
    private static ArrayList<LngLat[]> regionEdgesBuilder(NamedRegion region) {
        ArrayList<LngLat[]> regionEdges = new ArrayList<>(region.vertices().length);
        for (int i = 0; i < region.vertices().length; i++) {
            LngLat[] tuple;
            if (i < region.vertices().length - 1) {
                tuple = new LngLat[]{region.vertices()[i], region.vertices()[i + 1]};
            }
            else {
                tuple = new LngLat[]{region.vertices()[i], region.vertices()[0]};
            }
            regionEdges.add(tuple);
        }
        return regionEdges;
    }

    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        double resultLng = startPosition.lng();
        double resultLat = startPosition.lat();
        if (angle != 999.0) {
            resultLat += SystemConstants.DRONE_MOVE_DISTANCE*sin(toRadians(angle));
            resultLng += SystemConstants.DRONE_MOVE_DISTANCE*cos(toRadians(angle));
        }
        return (new LngLat(resultLng, resultLat));
    }

    public LngLat[] getBounds(ArrayList<LngLat> vertices) {
        double min_lng = Double.MAX_VALUE;
        double max_lng = Double.MIN_VALUE;
        double min_lat = Double.MAX_VALUE;
        double max_lat = Double.MIN_VALUE;
        for (LngLat vertex: vertices) {
            if (vertex.lng() < min_lng) {
                min_lng = vertex.lng();
            }

            if (vertex.lng() > max_lng) {
                max_lng = vertex.lng();
            }

            if (vertex.lat() < min_lat) {
                min_lat = vertex.lat();
            }

            if (vertex.lat() > max_lat) {
                max_lat = vertex.lat();
            }
        }
        return new LngLat[]{new LngLat(min_lng, min_lat), new LngLat(max_lng, max_lat)};
    }

    public ArrayList<LngLat> getNeighbours(LngLat coord, int dir, double step, LngLat bound1, LngLat bound2) {
        ArrayList<LngLat> neighbours = new ArrayList<>();
        double angle = 0.0;
        double angle_increment = 2 * PI / dir;
        while (angle < 2*PI) {
            LngLatHandler lngLatHandler = new LngLatHandler();
            LngLat neighbour = nextPosition(coord, angle);
            angle += angle_increment;
            if (neighbour.lng() < min(bound1.lng(), bound2.lng())) { continue;}
            if (neighbour.lng() > max(bound1.lng(), bound2.lng())) { continue;}
            if (neighbour.lat() < min(bound1.lat(), bound2.lat())) { continue;}
            if (neighbour.lat() > max(bound1.lat(), bound2.lat())) { continue;}
            neighbours.add(neighbour);
        }
        return neighbours;
    }
}
