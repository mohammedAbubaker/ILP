package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.*;

import java.io.File;
import java.time.DayOfWeek;
import java.util.ArrayList;

// JSON Deserializing
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.pathfinder.DronePathFinder;
import uk.ac.ed.inf.serializers.Serializer;
import uk.ac.ed.inf.serializers.LocalDateDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

// Order Validation
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.validator.OrderValidator;

// Connection Handling
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;

/**
 * Holds all the orders that need to be processed, as well as the information required to process that order.
 * */
public class Context {
    // date refers to command line arguments, not order dates
    private final String date;
    private final String url;
    // values are retrieved with get()
    private Restaurant[] restaurants;
    private NamedRegion centralArea;
    private NamedRegion[] noFlyZones;
    private ArrayList<Order> orders;
    // defaults to Appleton Towers
    private LngLat src = new LngLat(-3.186874, 55.944494);
    // refers to restaurant location (represents the drone's destination)
    private LngLat dest;


    /** Context class represents all the data collected from a REST Server given a specific day.
     *  Upon construction, Context attempts to load all the order data from the REST Server.
     *  For each order, context uses {@link OrderValidator} to handle order validation.
     *  If the order is valid, Context uses {@link DronePathFinder} to handle flight path computation.
     *  If the Context was provided with no date and just a URL, then it will get all orders on the server.
     * @param url Valid url to the homepage of a REST server, where JSON endpoints are accessible by appending a '/endpoint'.
     * @param date Valid date.
     */
    public Context(String url, String date) {
        this.url = url;
        this.date = date;
        Serializer serializer = new Serializer();
        // load data
        long get_start_time = System.nanoTime();
        get();
        long get_end_time = System.nanoTime();
        System.out.println("Retrieving from url took: " + ((get_end_time - get_start_time) / 1_000_000_000.0) + "s");

        long start_time = System.nanoTime();
        // initialise orderValidator
        OrderValidator orderValidator = new OrderValidator();
        // initialise path caching
        HashMap<LngLat, ArrayList<LngLat>> pathCache = new HashMap<>();
        int pathfinder_called = 0;
        // order processing done here
        for (Order order: this.orders) {
            // pass order to order validator
            Order processedOrder = orderValidator.validateOrder(order, this.restaurants);
            // if order is invalid then add to the delivery serializer and then skip
            if (!processedOrder.getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                serializer.addToDeliverySerializer(processedOrder, new LngLat(0.0, 0.0), new LngLat(0.0, 0.0));
                continue;
            }
            // if order is valid then get restaurant
            Restaurant restaurant = getRestaurantFromOrder(processedOrder, restaurants);
            // set destination to restaurant location
            this.dest = restaurant.location();

            if (!pathCache.containsKey(dest)) {
                // find the drone path with the current context
                System.out.println(restaurant.name());
                pathCache.put(dest, new DronePathFinder(this).getRoute());
            }

            pathCache.put(dest, new DronePathFinder(this).getRoute());
            pathfinder_called += 1;
            LngLat[] dronePath = pathCache.get(dest).toArray(new LngLat[0]);
            // add information to serializer
            serializer.addToFlightPathSerializer(processedOrder, dronePath);
            serializer.addToDeliverySerializer(processedOrder, this.getSrc(), this.getDest());
            serializer.addToFeatureCollection(dronePath);
        }
        System.out.println(pathfinder_called);
        long end_time = System.nanoTime();
        System.out.println("Routing took: " + ((end_time - start_time) / 1_000_000_000.0) + "s");
        long start_time_serializing = System.nanoTime();
        // serialize to drone-date.json, flightpath-date.json, deliveries-date.json
        serializer.serialize(date);
        long end_time_serializing = System.nanoTime();
        // print routing time
        System.out.println("Serializing took: " + ((end_time_serializing - start_time_serializing) / 1_000_000_000.0) + "s");

    }

    /**
     * This function is analogous to HTTP's GET. Retrieves information from the necessary endpoints and stores it in the
     * caller's fields. If the endpoint is empty then notify the user with {@link System#err}. If the URL is invalid
     * notify the user.
     */
    private void get() {

        // initialise values
        this.orders = new ArrayList<>();
        this.restaurants = new Restaurant[]{};
        this.noFlyZones = new NamedRegion[]{};
        this.centralArea = new NamedRegion("", new LngLat[0]);

        // set up custom local date deserializing
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeserialize());
        objectMapper.registerModule(module);

        try {
            // check if user wants all orders or orders filtered by a date passed in as an argument from the terminal
            String order_string = "orders";
            if (!this.date.isBlank()) {
                order_string += ('/' + this.date);
            }
            // read values
            this.orders = objectMapper.readValue(fetchDataFromService(order_string), new TypeReference<ArrayList<Order>>() {});
            this.restaurants = objectMapper.readValue(fetchDataFromService("restaurants"), Restaurant[].class);
            this.noFlyZones = objectMapper.readValue(fetchDataFromService("noFlyZones"), NamedRegion[].class);
            // this.nolyZones = objectMapper.readValue(new File("namedregions.json"), NamedRegion[].class);
            this.centralArea = objectMapper.readValue(fetchDataFromService("centralArea"), NamedRegion.class);

            // notify if values are empty
            if (this.orders.isEmpty()) {
                System.err.println("Warning: /orders/" + date + " is empty");
            }
            if (this.restaurants.length == 0) {
                System.err.println("Warning: /restaurants is empty");
            }
            if (this.noFlyZones.length == 0) {
                System.err.println("Warning: /noFlyZones is empty");
            }
            if (centralArea.vertices().length == 0) {
                System.err.println("Warning: /centralArea is empty");
            }
        }
        catch (Exception e) {
            System.err.println("Error: invalid url/endpoint provided. Please try different arguments");
        }
    }


    /**
     * This function appends an {@param endpoint} on {@link #url} and then attempts to retrieve the JSON string,
     * @param endpoint The specified endpoint
     * @return The JSON string at the endpoint, ready to be deserialized.
     */
    private String fetchDataFromService(String endpoint) {
        try {
            URL u = new URL(this.url + "/" + endpoint);
            InputStream input = u.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }
            return json.toString();
        }
        catch (Exception e) {
            System.err.println("Error: /" + endpoint + "/ is not a valid endpoint on: " + this.url);
            return "";
        }
    }

    /**
     * This function finds the restaurant corresponding to the order and returns it.
     * @param order An {@link Order}.
     * @param restaurants A list of {@link Restaurant}.
     * @return The {@link Restaurant} corresponding to that {@link Order}.
     */
    private static Restaurant getRestaurantFromOrder(Order order, Restaurant[] restaurants) {
        // get name of first pizza from order
        for (Restaurant restaurant: restaurants) {
            for (Pizza pizza: restaurant.menu()) {
                // return restaurant once match is found
                if (pizza.name().equals(order.getPizzasInOrder()[0].name())) {
                    return restaurant;
                }
            }
        }
        // return a blank restaurant to avoid unexpected behaviours introduced by dangling null pointers.
        return new Restaurant("", new LngLat(0.0, 0.0), new DayOfWeek[]{}, new Pizza[]{});
    }

    //------------------------------------------------------------------
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
    public NamedRegion getCentralArea() { return this.centralArea; }

    //------------------------------------------------------------------
    // Setters
    public void setSrc(LngLat src) {
        this.src = src;
    }

    public void setDest(LngLat dest) {
        this.dest = dest;
    }
}