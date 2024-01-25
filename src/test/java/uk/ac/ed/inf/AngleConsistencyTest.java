package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.pathfinder.LngLatHandler;
import uk.ac.ed.inf.serializers.FlightpathSerializer;

import java.io.File;

public class AngleConsistencyTest {
    @Test
    void validateAngles() {
        FlightpathSerializer[] flightpathSerializers = new FlightpathSerializer[0];
        LngLatHandler lngLatHandler = new LngLatHandler();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            flightpathSerializers = objectMapper.readValue(new File("flightpath.json"), FlightpathSerializer[].class);
            for (int i = 0; i < flightpathSerializers.length - 1; i++) {
                double currentAngle = flightpathSerializers[i].angle;
                LngLat currentPosition = new LngLat(flightpathSerializers[i].fromLongitude, flightpathSerializers[i].fromLatitude);
                LngLat nextPosition = new LngLat(flightpathSerializers[i+1].fromLongitude, flightpathSerializers[i+1].fromLatitude);
                if (flightpathSerializers[i].angle == 999.0) {
                    assert currentPosition.equals(nextPosition);
                    continue;
                }
                if (!(lngLatHandler.nextPosition(currentPosition, currentAngle).equals(nextPosition))) {
                    System.out.println(currentPosition);
                    System.out.println(currentAngle);
                    System.out.println(nextPosition);
                }
                assert ((lngLatHandler.
                        distanceTo(lngLatHandler.nextPosition(currentPosition, currentAngle), nextPosition) < 0.0001));
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
