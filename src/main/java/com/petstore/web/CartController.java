package com.petstore.web;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.petstore.cart.CartService;
import com.petstore.cart.ShoppingCart;

/**
 * The legacy cart.do changed the cart on GET (?action=purchase&itemId=...). Changes are POSTs now, behind
 * CSRF protection, and redirect back so a refresh doesn't repeat them.
 */
@Controller
class CartController {

    static final String QUANTITY_PREFIX = "itemQuantity_";

    private final CartService cartService;
    private final ShoppingCart cart;

    CartController(CartService cartService, ShoppingCart cart) {
        this.cartService = cartService;
        this.cart = cart;
    }

    @GetMapping({"/cart.do", "/cart.screen"})
    String show(Locale locale, Model model) {
        model.addAttribute("cart", cartService.view(cart, locale));
        return Views.render(model, Screen.CART);
    }

    @PostMapping(value = "/cart.do", params = "action=purchase")
    String add(@RequestParam String itemId, Locale locale) {
        cartService.add(cart, itemId, locale);
        return "redirect:/cart.do";
    }

    @PostMapping(value = "/cart.do", params = "action=remove")
    String remove(@RequestParam String itemId) {
        cart.remove(itemId);
        return "redirect:/cart.do";
    }

    @PostMapping(value = "/cart.do", params = "action=update")
    String update(@RequestParam Map<String, String> params) {
        Map<String, String> quantities = new LinkedHashMap<>();
        params.forEach((name, value) -> {
            if (name.startsWith(QUANTITY_PREFIX)) {
                quantities.put(name.substring(QUANTITY_PREFIX.length()), value);
            }
        });
        cartService.updateQuantities(cart, quantities);
        return "redirect:/cart.do";
    }
}
