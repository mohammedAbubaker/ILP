package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class TestUtils {
    public static CreditCardInformation getSampleCreditCardInformation() {
        return new CreditCardInformation("1234567890123456", "12/26", "123");
    }

    public static Pizza[] getSamplePizzaOrder() {
        Pizza[] pizza = {new Pizza("Margherita", 900), new Pizza("Calzone", 400)};
        return pizza;
    }

    public static Order getSampleOrder() {
        Order order = new Order("123", LocalDate.of(2023, 10, 12), 1400, getSamplePizzaOrder(), getSampleCreditCardInformation());
        order.setCreditCardInformation(getSampleCreditCardInformation());
        return order;
    }

    public static Pizza[] getMosMenu() {
        Pizza[] pizzas = {new Pizza("Margherita", 900), new Pizza("Calzone", 400)};
        return pizzas;
    }

    public static Pizza[] getCiverinosMenu() {
        Pizza[] pizzas = {new Pizza("Veggie", 900), new Pizza("Meat", 400)};
        return pizzas;
    }

    public static DayOfWeek[] getDaysOfWeek() {
        DayOfWeek[] dayOfWeek = {DayOfWeek.of(4), DayOfWeek.of(5), DayOfWeek.of(6), DayOfWeek.of(7)};
        return dayOfWeek;
    }

    public static Restaurant getMosRestaurant() {
        return new Restaurant("Mo's Special Delivery", new LngLat(0.0, 0.0), getDaysOfWeek(), getMosMenu());
    }

    public static Restaurant getCiverinosRestaurant() {
        return new Restaurant("Civerinos", new LngLat(55.949709102995236, -3.187916680170299), getDaysOfWeek(), getCiverinosMenu());
    }

    public static Restaurant[] getSampleRestaurants() {
        Restaurant[] restaurants = {getMosRestaurant(), getCiverinosRestaurant()};
        return restaurants;
    }

    public static Context getSampleContext() {
        Context context = new Context("", "");
        return context;
    }
}
