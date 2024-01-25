package uk.ac.ed.inf;

import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.Order;

import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.validator.OrderValidator;

import javax.swing.plaf.BorderUIResource;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;

import static uk.ac.ed.inf.TestUtils.*;

public class OrderValidatorTest {
    // ------------------------------------------------------------------
    // Null Checking Section
    @Test
    void testNullPizzaList() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        order.setPizzasInOrder(null);
        assert (OrderValidationCode.PIZZA_NOT_DEFINED == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testNullOrderNo() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        order.setOrderNo(null);
        order.setPizzasInOrder(getSamplePizzaOrder());
        assert (OrderStatus.INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderStatus());
    }

    @Test
    void testNullOrderDate() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        order.setOrderDate(null);
        order.setPizzasInOrder(getSamplePizzaOrder());
        assert (OrderStatus.INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderStatus());
    }

    @Test
    void testNullCreditCardInformation() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        order.setCreditCardInformation(null);
        assert (OrderStatus.INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderStatus());
    }

    @Test
    void testNullCreditCardExpiryDate() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardExpiry(null);
        order.setCreditCardInformation(creditCardInformation);
        assert( OrderValidationCode.EXPIRY_DATE_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testNullCreditCardNumber() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardNumber(null);
        order.setCreditCardInformation(creditCardInformation);
        assert ( OrderValidationCode.CARD_NUMBER_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testNullCVV() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCvv(null);
        order.setCreditCardInformation(creditCardInformation);
        assert ( OrderValidationCode.CVV_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }
    //------------------------------------------------------------------


    @Test
    void testEmptyPizzaList() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        Pizza[] pizza = {};
        order.setPizzasInOrder(pizza);
        assert (OrderValidationCode.PIZZA_NOT_DEFINED == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testCreditCardNumber1() {
        // Test invalid length
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardNumber("123456789012345678");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.CARD_NUMBER_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testCreditCardNumber2() {
        // Test invalid character
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardNumber("123456789 123456");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.CARD_NUMBER_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testExpiryDate1() {
        // Test if expiry date fits expected pattern
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardExpiry("12:25");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.EXPIRY_DATE_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testExpiryDate2() {
        // Test it recognises correct months
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardExpiry("13/25");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.EXPIRY_DATE_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testExpiryDate3() {
        // Test it recognises past dates
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCreditCardExpiry("02/97");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.EXPIRY_DATE_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testCreditCardCVV1() {
        // Test invalid length
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCvv("1234");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.CVV_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testCreditCardCVV2() {
        // Test invalid character
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        CreditCardInformation creditCardInformation = getSampleCreditCardInformation();
        creditCardInformation.setCvv("12A");
        order.setCreditCardInformation(creditCardInformation);
        assert (OrderValidationCode.CVV_INVALID == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testOrderTotal() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        order.setPriceTotalInPence(1500);
        assert (OrderValidationCode.TOTAL_INCORRECT == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());

    }

    @Test
    void testPizzaOrderExists() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        Pizza[] pizza = {order.getPizzasInOrder()[0], new Pizza("Lebron James", 400)};
        order.setPizzasInOrder(pizza);
        assert (OrderValidationCode.PIZZA_NOT_DEFINED == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testPizzasMultipleRestaurants() {
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        Pizza[] pizza = {getCiverinosRestaurant().menu()[0], getMosRestaurant().menu()[1]};
        order.setPizzasInOrder(pizza);
        assert (OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testRestaurantClosed() {
        // The order date is on a Thursday, the restaurant is open on a Saturday and Sunday.
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        Restaurant[] restaurants = getSampleRestaurants();
        DayOfWeek[] openingDays = {DayOfWeek.of(6), DayOfWeek.of(7)};
        Restaurant restaurant = new Restaurant(restaurants[0].name(), restaurants[0].location(), openingDays, restaurants[0].menu());
        restaurants[0] = restaurant;
        assert (OrderValidationCode.RESTAURANT_CLOSED == orderValidator.validateOrder(order, restaurants).getOrderValidationCode());
    }

    @Test
    void testMaxCountPizza() {
        // Test the correct flagging of exceeding pizza count
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        Pizza[] pizzas = getSamplePizzaOrder();
        ArrayList<Pizza> pizza = new ArrayList<>();
        for (Pizza piz: pizzas) {
            pizza.add(piz);
            pizza.add(piz);
            pizza.add(piz);
        }
        order.setPizzasInOrder(pizza.toArray(new Pizza[0]));
        assert (OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }

    @Test
    void testValidOrder() {
        // The order date is on a Thursday, the restaurant is open on a Saturday and Sunday.
        OrderValidator orderValidator = new OrderValidator();
        Order order = getSampleOrder();
        assert (OrderValidationCode.NO_ERROR == orderValidator.validateOrder(order, getSampleRestaurants()).getOrderValidationCode());
    }
}