package uk.ac.ed.inf.validator;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;
import uk.ac.ed.inf.ilp.data.Pizza;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;

/**
    Given an order and a list of restaurants,
    determine and set the validation code and status code of the order,
    then return the order.
 */
public class OrderValidator implements OrderValidation {
    @Override
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {
        // --------------------------------------------------------------------
        // Null checking section
        if (orderToValidate.getPizzasInOrder() == null) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (orderToValidate.getOrderNo() == null) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (orderToValidate.getOrderDate() == null) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (orderToValidate.getCreditCardInformation() == null)  {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (orderToValidate.getCreditCardInformation().getCreditCardExpiry() == null) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (orderToValidate.getCreditCardInformation().getCreditCardNumber() == null) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (orderToValidate.getCreditCardInformation().getCvv() == null) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        // --------------------------------------------------------------------

        // *** Check if too many pizzas were ordered
        if (orderToValidate.getPizzasInOrder().length > SystemConstants.MAX_PIZZAS_PER_ORDER) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }


        // Check if pizza list is empty
        if (orderToValidate.getPizzasInOrder().length == 0) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        CreditCardInformation creditCardInformation = orderToValidate.getCreditCardInformation();
        /*
         *** Check if card number is incorrect
         Check if card number is 16 characters
         Check if string contains non-numeric character
        */
        if (!creditCardInformation.getCreditCardNumber().matches("[0-9]{16}")) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        /*
         *** Check if expiry date is valid
         Check expiry date fits the expected pattern: MM/YY
         Reject invalid month values like 13
        */
        if (!orderToValidate.getCreditCardInformation().getCreditCardExpiry().matches("(?:0[1-9]|1[0-2])[/][0-9]{2}")) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        // Check card is not expired
        LocalDate date = orderToValidate.getOrderDate();
        int expiryDateMonth = Integer.parseInt(
                creditCardInformation.getCreditCardExpiry().substring(0,2)
        );
        int expiryDateYear = Integer.parseInt(
                creditCardInformation.getCreditCardExpiry().substring(3,5)
        );

        /*
         Expiry year 97 refers to 1997, expiry year 20 refers to 2020 so
         if expiry year > current year + 20,
         reject since most credit card companies don't issue cards that expire in more than 20 years.
         And if expiry year < current year, reject.
        */

        if (expiryDateYear < (date.getYear() - 2000)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        if (expiryDateYear > (date.getYear() - 2000 + 20)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        // check month if year is the same
        if (expiryDateMonth < date.getMonthValue() && (date.getYear() - 2000 == expiryDateYear)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        /*
         *** Check if CVC is wrong
         - Check that it is length 3
         - Check that it is numeric
        */

        if (!creditCardInformation.getCvv().matches("[0-9]{3}")) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        // *** Check if the order total is incorrect
        int trueTotalPrice = SystemConstants.ORDER_CHARGE_IN_PENCE;
        for (Pizza pizza : orderToValidate.getPizzasInOrder()) {
            trueTotalPrice += pizza.priceInPence();
        }
        if (trueTotalPrice != orderToValidate.getPriceTotalInPence()) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }



        // *** Check if no such pizza exists

        /*
        Due to pizza names being unique system-wide,
        construct a hashmap using pizza names as keys and restaurants as values.
        */
        HashMap<String, Restaurant> pizzaToRestaurant = new HashMap();
        for (Restaurant restaurant: definedRestaurants) {
            for (Pizza pizza: restaurant.menu()) {
                pizzaToRestaurant.put(pizza.name(), restaurant);
            }
        }

        boolean pizzaExists = true;
        for (Pizza pizza: orderToValidate.getPizzasInOrder()) {
            if (!pizzaToRestaurant.containsKey(pizza.name())) {
                pizzaExists = false;
            }
            if (!pizzaExists) {
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                return orderToValidate;
            }
        }

        // *** Check if pizzas were ordered from multiple restaurants

        Restaurant initialRestaurant = pizzaToRestaurant.get(orderToValidate.getPizzasInOrder()[0].name());

        for (Pizza pizza: orderToValidate.getPizzasInOrder()) {
            if (!pizzaToRestaurant.get(pizza.name()).name().equals(initialRestaurant.name())) {
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                return orderToValidate;
            }
        }

        // *** Check if the restaurant is closed on the order day
        if (!Arrays.asList(initialRestaurant.openingDays()).contains(orderToValidate.getOrderDate().getDayOfWeek())) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        return orderToValidate;
    }
}
