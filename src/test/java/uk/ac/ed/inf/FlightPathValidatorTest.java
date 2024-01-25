package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.pathfinder.LngLatHandler;

import java.util.ArrayList;

public class FlightPathValidatorTest {
    /*
    * Given a flightpath array,
    * noflyzone, src and dest, make sure that a point never touches a nofly zone, while also
    * validating that the point arrives at the mentioned points.
    * */
    public static boolean validateFlightPath(ArrayList<LngLat> flightpath, NamedRegion[] noFlyZone, LngLat src, LngLat dest) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        ArrayList<Integer> points = new ArrayList<>();
        for (int i = 0; i < flightpath.size(); i++) {
            if (lngLatHandler.isCloseTo(flightpath.get(i), src)) {
                points.add(i);
            }
            if (lngLatHandler.isCloseTo(flightpath.get(i), dest)) {
                points.add(i);
            }
            if (lngLatHandler.isInRegions(flightpath.get(i), noFlyZone)) {
                return false;
            }
        }
        return true;
    }
}
