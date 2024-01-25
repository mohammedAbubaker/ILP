package uk.ac.ed.inf;

// Purpose of Test:
// Verify that exactly zero points of a generated flightpath are inside a no-fly zone.

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.pathfinder.LngLatHandler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PathWalkerTest {
    @Test
    void testDronePath() {
        try {
            // load noflyzones in memory
            ObjectMapper mapper = new ObjectMapper();
            NamedRegion[] noFlyZones = mapper.readValue(Paths.get("namedregions.json").toFile(), NamedRegion[].class);

            // load in filepath json in memory.
            String flightpaths = Files.readString(Paths.get("flightpath.json"));
            String regex = "(fromLatitude\":\\d+\\.\\d+)|(fromLongitude\":-\\d+\\.\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(flightpaths);
            ArrayList<Double> latitudes = new ArrayList<Double>();
            ArrayList<Double> longitudes = new ArrayList<Double>();
            // collect latitudes and longitudes into their respective bins
            while (matcher.find()) {
                String fromLatitude = matcher.group(1);
                String fromLongitude = matcher.group(2);

                if (fromLatitude != null) {
                    latitudes.add(Double.parseDouble(fromLatitude.replaceAll("fromLatitude\":", "")));
                }

                if (fromLongitude != null) {
                    longitudes.add(Double.parseDouble(fromLongitude.replaceAll("fromLongitude\":", "")));
                }
            }
            // check that latitudes == longitudes
            assert (latitudes.size() == longitudes.size());


            boolean pointInRegion = false;
            // create lgnlat handler
            LngLatHandler lngLatHandler = new LngLatHandler();
            for (int i = 0; i < latitudes.size(); i++) {
                LngLat point = new LngLat(latitudes.get(i), longitudes.get(i));
                if (lngLatHandler.isInRegions(point, noFlyZones)) {
                    pointInRegion = true;
                    break;
                }
            }
            assert pointInRegion == false;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}