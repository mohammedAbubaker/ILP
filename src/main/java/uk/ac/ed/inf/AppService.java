package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.module.SimpleModule;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ed.inf.utils.LocalDateDeserialize;

import java.io.BufferedReader;
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
        long start_time = System.nanoTime();
        VisibilityGraph visibilityGraph = new VisibilityGraph(this);

        // initialise orderValidator]
        OrderValidator orderValidator = new OrderValidator();
        // store all orders with restaurant names
        for (Order order: this.orders) {
            // skip if order does not correspond to the provided date
            if (!order.getOrderDate().isEqual(this.date)) {
                continue;
            }
            // skip if order fails validation
            if (!orderValidator.validateOrder(order, this.restaurants).getOrderValidationCode().equals(OrderValidationCode.NO_ERROR)) {
                continue;
            }
            LngLat destination = getRestaurantFromOrder(order, restaurants).location();
            if (destination != null) {
                visibilityGraph.updateViGraph(this,  destination);
                visibilityGraph.plotGraph();
                this.dest = destination;
                // handle pathfinding

            }
        }
        long end_time = System.nanoTime();
        System.out.println((end_time - start_time) / 1_000_000_000.0);
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
}
