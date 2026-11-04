package com.petstore.cart;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Item ids and quantities only - prices are looked up at render time in the shopper's current locale,
 * as the legacy stateful ShoppingCart EJB did. Insertion-ordered so the cart doesn't reshuffle.
 */
@Component
@SessionScope
public class ShoppingCart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Integer> quantities = new LinkedHashMap<>();

    /** Adding an item that is already in the cart resets it to 1, same as legacy. */
    public synchronized void add(String itemId) {
        quantities.put(itemId, 1);
    }

    public synchronized void remove(String itemId) {
        quantities.remove(itemId);
    }

    public synchronized void updateQuantity(String itemId, int quantity) {
        if (!quantities.containsKey(itemId)) {
            return;
        }
        if (quantity > 0) {
            quantities.put(itemId, quantity);
        } else {
            quantities.remove(itemId);
        }
    }

    public synchronized Map<String, Integer> contents() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(quantities));
    }

    public synchronized int size() {
        return quantities.size();
    }

    public synchronized boolean isEmpty() {
        return quantities.isEmpty();
    }

    public synchronized void clear() {
        quantities.clear();
    }
}
