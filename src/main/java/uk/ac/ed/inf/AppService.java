package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.module.SimpleModule;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.inf.utils.LocalDateDeserialize;

import java.time.LocalDate;
import java.util.ArrayList;

/*
The AppService establishes a connection to a RESTServer,
and stores the necessary information inside its structure.
* */
@Service
public class AppService {
    private final RestTemplate restTemplate;
    private final String url;
    private final LocalDate date;
    private Restaurant[] restaurants;
    private NamedRegion centralArea;
    private NamedRegion[] noFlyZones;
    private ArrayList<Order> orders;

    @Autowired
    public AppService(String url, String date) {
        this.restTemplate = new RestTemplate();
        this.url = url;
        this.date= LocalDate.parse(date);
    }

    /*
    Given the name of a JSON webpage (i.e. restaurants, orders),
    retrieve the JSON string
    * */
    private String fetchDataFromService(String data) {
        return this.restTemplate.getForObject(this.url + "/" + data, String.class);
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
            this.orders = objectMapper.readValue(fetchDataFromService("orders"), new TypeReference<>() {
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
        // initialise orderValidator
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
            // initialise drone pathfinder
            DronePathFinder dronePathFinder = new DronePathFinder();
            // set src to Appleton Tower longitude and latitude
            dronePathFinder.setSrc(new LngLat(-3.186874,55.944494));
            // set dest to restaurant location
            dronePathFinder.setDest(getRestaurantFromOrder(order, restaurants).location());
            // set geometry
            dronePathFinder.setNoFlyZones(this.noFlyZones);
            dronePathFinder.setCentralArea(this.centralArea);
            // execute pathfinding algorithm
            dronePathFinder.pathfind();
        }
    }
}
