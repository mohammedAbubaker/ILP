package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.module.SimpleModule;
import uk.ac.ed.inf.geojson.Feature;
import uk.ac.ed.inf.geojson.GeoJson;
import uk.ac.ed.inf.geojson.Geometry;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.utils.LocalDateDeserialize;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

/*
The AppService establishes a connection to a RESTServer,
and stores the necessary information inside its structure.
* */
public class AppService {
    private final String url;
    private final LocalDate date;
    private Restaurant[] restaurants;
    private NamedRegion centralArea;
    private NamedRegion[] noFlyZones;
    private ArrayList<Order> orders;
    // default value is Appleton Towers
    private LngLat src = new LngLat(-3.186874, 55.944494);
    private LngLat dest;

    public AppService(String url, String date) {
        this.url = url;
        this.date= LocalDate.parse(date);
    }

    /*
    Given the name of a JSON webpage (i.e. restaurants, orders),
    retrieve the JSON string
    * */
    private String fetchDataFromService(String data) {
        try {
            URL u = new URL(this.url + "/" + data);
            InputStream input = u.openStream();
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }
            return json.toString();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "";
    }
    /*
    Convert JSON string into Java objects and store within class.
    * */
    private void parseData() {
        // set up custom local date deserializing
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeserialize());
        objectMapper.registerModule(module);
        // read in values and store
        try {
            this.orders = objectMapper.readValue(fetchDataFromService("orders"), new TypeReference<ArrayList<Order>>() {
            });
            this.restaurants = objectMapper.readValue(fetchDataFromService("restaurants"), Restaurant[].class);
            this.noFlyZones = objectMapper.readValue(fetchDataFromService("noFlyZones"), NamedRegion[].class);
            this.centralArea = objectMapper.readValue(fetchDataFromService("centralArea"), NamedRegion.class);
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /*
    Must be executed after orderValidator to prevent null pointer
    * */
    private Restaurant getRestaurantFromOrder(Order order, Restaurant[] restaurants) {
        // get name of first pizza from order
        for (Restaurant restaurant: restaurants) {
            for (Pizza pizza: restaurant.menu()) {
                // return restaurant once match is found
                if (pizza.name().equals(order.getPizzasInOrder()[0].name())) {
                    return restaurant;
                }
            }
        }
        return null;
    }

    public void runAppService() {
        // retrieve data
        parseData();
        ArrayList<Feature> featureCollection = new ArrayList<>();
        featureCollection.add(new Feature(new Geometry(this.centralArea.vertices(), "Polygon"), "centralArea"));
        for (NamedRegion zone: this.noFlyZones) {
            featureCollection.add(new Feature(new Geometry(zone.vertices(), "Polygon"), "noFlyZone"));
        }
        int success_full = 0;
        int n_orders = 0;
        long start_time = System.nanoTime();
        // initialise orderValidator]
        OrderValidator orderValidator = new OrderValidator();
        // store all orders with restaurant names
        for (Order order: this.orders) {
            // skip if order does not correspond to the provided date
            //if (!order.getOrderDate().isEqual(this.date)) {
            //    continue;
            //}
            // skip if order fails validation
            if (!orderValidator.validateOrder(order, this.restaurants).getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                continue;
            }
            this.dest = getRestaurantFromOrder(order, restaurants).location();
            if (this.dest != null) {
                DronePathFinder dronePathFinder = new DronePathFinder(this);
                LngLat[] dronePath = dronePathFinder.getRoute();
                if (dronePath.length > 1) {
                    System.out.println("done");
                    success_full += 1;
                }
                featureCollection.add(new Feature(new Geometry(dronePath, "LineString"), "dronePath"));
            }
            n_orders += 1;
        }
        long end_time = System.nanoTime();
        System.out.println((end_time - start_time) / 1_000_000_000.0);
        System.out.println("n_orders " + n_orders);
        System.out.println("successful orders " + success_full);
        plotGraph(featureCollection);
    }

    // ------------------------------------------------------------------
    // Getters
    public LngLat getSrc() {
        return this.src;
    }

    public LngLat getDest() {
        return this.dest;
    }
    public NamedRegion getCentralArea() {
        return this.centralArea;
    }
    public NamedRegion[] getNoFlyZones() {
        return this.noFlyZones;
    }

    void addGraph(ArrayList<Feature> featureCollection, DronePathFinder dronePathFinder) {
        for (String edge: dronePathFinder.visibilityGraph.edges) {
            featureCollection.add(
                    new Feature(
                            new Geometry(new LngLat[]{dronePathFinder.visibilityGraph.nodes.get(Integer.parseInt(edge.split(" ")[0])), dronePathFinder.visibilityGraph.nodes.get(Integer.parseInt(edge.split(" ")[1]))}, "LineString"), "vigraphLines"));
        }
    }
    void plotGraph(ArrayList<Feature> featureCollection) {
        // generate feature collection
        // add central area polygon
        // add noflyzone polygons
        GeoJson geojson = new GeoJson(featureCollection.toArray(Feature[]::new));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            String json = objectMapper.writeValueAsString(geojson);
            FileOutputStream fileOutputStream = new FileOutputStream("all.json");
            fileOutputStream.write(json.getBytes()); }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
