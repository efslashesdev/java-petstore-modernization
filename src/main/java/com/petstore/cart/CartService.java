package com.petstore.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Item;

@Service
public class CartService {

    static final int MAX_QUANTITY = 9_999;

    private final CatalogService catalog;

    public CartService(CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Lines are priced at list price - that is what the legacy cart charged, even though the item page
     * labels the lower unit cost as "Your Price". Kept for parity; see the README.
     */
    public CartView view(ShoppingCart cart, Locale locale) {
        List<CartLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cart.contents().entrySet()) {
            catalog.item(entry.getKey(), locale)
                    .ifPresent(item -> lines.add(new CartLine(item, entry.getValue())));
        }
        return new CartView(lines);
    }

    public void add(ShoppingCart cart, String itemId, Locale locale) {
        // Don't let arbitrary ids into the session.
        if (catalog.item(itemId, locale).isPresent()) {
            cart.add(itemId);
        }
    }

    /** Unparseable quantities count as 0 (which removes the line) - legacy behaviour. */
    public void updateQuantities(ShoppingCart cart, Map<String, String> rawQuantities) {
        rawQuantities.forEach((itemId, raw) -> cart.updateQuantity(itemId, parseQuantity(raw)));
    }

    static int parseQuantity(String raw) {
        try {
            return Math.min(Integer.parseInt(raw.trim()), MAX_QUANTITY);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    public record CartLine(Item item, int quantity) {
        public BigDecimal unitPrice() {
            return item.listPrice();
        }

        public BigDecimal total() {
            return item.listPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record CartView(List<CartLine> lines) {
        public BigDecimal subTotal() {
            return lines.stream().map(CartLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public boolean isEmpty() {
            return lines.isEmpty();
        }
    }
}
