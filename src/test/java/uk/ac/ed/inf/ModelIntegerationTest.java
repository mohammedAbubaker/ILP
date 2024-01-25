package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.pathfinder.DronePathFinder;
import uk.ac.ed.inf.validator.OrderValidator;

import java.nio.file.Paths;
import java.util.ArrayList;

import static uk.ac.ed.inf.FlightPathValidatorTest.validateFlightPath;
import static uk.ac.ed.inf.TestUtils.*;

public class ModelIntegerationTest {
    // Construct a sample order
    // Then run orderValidate on it.
    // Then run dronePathfinder.getRoute on it.
    // Test if dronePathFinder is accurate by: drone simulation
    // Then serialise path
    @Test
    void performIntegrationTest() {
        // construct a sample order
        Order sampleOrder = getSampleOrder();

        // test validation
        OrderValidator orderValidator = new OrderValidator();
        orderValidator.validateOrder(sampleOrder, getSampleRestaurants());
        assert sampleOrder.getOrderValidationCode() == OrderValidationCode.NO_ERROR;

        // drone path validation
        Context sampleContext = getSampleContext();
        sampleContext.setSrc(new LngLat(55.94450099032128, -3.1870685189557175));
        sampleContext.setDest(getCiverinosRestaurant().location());
        ObjectMapper mapper = new ObjectMapper();
        try {
            NamedRegion[] noFlyZones = mapper.readValue(Paths.get("namedregions.json").toFile(), NamedRegion[].class);
            ArrayList<LngLat> flightpath = new DronePathFinder(sampleContext).getRoute();
            System.out.println("Executing Path Validation..");
            assert validateFlightPath(flightpath, noFlyZones, sampleContext.getSrc(), sampleContext.getDest());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}