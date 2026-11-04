package com.petstore.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.petstore.cart.ShoppingCart;
import com.petstore.customer.Customer;
import com.petstore.order.OrderService;
import com.petstore.order.PurchaseOrder;
import com.petstore.web.form.OrderForm;

import jakarta.validation.Valid;

@Controller
class CheckoutController {

    private final OrderService orders;
    private final CurrentCustomer currentCustomer;
    private final ShoppingCart cart;

    CheckoutController(OrderService orders, CurrentCustomer currentCustomer, ShoppingCart cart) {
        this.orders = orders;
        this.currentCustomer = currentCustomer;
        this.cart = cart;
    }

    @GetMapping("/enter_order_information.screen")
    String orderForm(Model model) {
        model.addAttribute("orderForm", OrderForm.prefilled(currentCustomer.require().contactInfo()));
        return Views.render(model, Screen.ENTER_ORDER_INFORMATION);
    }

    /**
     * The legacy page rendered the confirmation straight from the POST, so a refresh re-submitted the order
     * (its "cart empty" error page even apologises for this). Post/redirect/get instead.
     */
    @PostMapping("/order.do")
    String placeOrder(@Valid @ModelAttribute OrderForm orderForm, BindingResult errors, Locale locale,
                      Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return Views.render(model, Screen.ENTER_ORDER_INFORMATION);
        }
        Customer customer = currentCustomer.require();
        PurchaseOrder order = orders.placeOrder(customer.userId(), orderForm.getBillTo().toContactInfo(),
                orderForm.getShipTo().toContactInfo(), customer.creditCard(), cart, locale);
        redirect.addFlashAttribute("orderId", order.orderId());
        redirect.addFlashAttribute("orderEmail", order.email());
        return "redirect:/order_complete.screen";
    }

    @GetMapping("/order_complete.screen")
    String orderComplete(Model model) {
        if (!model.containsAttribute("orderId")) {
            return "redirect:/main.screen";
        }
        return Views.render(model, Screen.ORDER_COMPLETE);
    }
}
