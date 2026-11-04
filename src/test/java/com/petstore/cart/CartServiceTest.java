package com.petstore.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Item;

class CartServiceTest {

    private final CatalogService catalog = mock(CatalogService.class);
    private final CartService service = new CartService(catalog);
    private final ShoppingCart cart = new ShoppingCart();

    @Test
    void linesArePricedAtListPriceInTheShoppersLocale() {
        stubItem("EST-1", "16.50", "10.00");
        stubItem("EST-2", "18.50", "12.00");
        cart.add("EST-1");
        cart.add("EST-2");
        cart.updateQuantity("EST-2", 3);

        var view = service.view(cart, Locale.US);

        assertThat(view.lines()).extracting(l -> l.item().itemId()).containsExactly("EST-1", "EST-2");
        assertThat(view.subTotal()).isEqualByComparingTo("72.00");
    }

    @Test
    void addingAnUnknownItemIsIgnored() {
        when(catalog.item(eq("NOPE"), any())).thenReturn(Optional.empty());

        service.add(cart, "NOPE", Locale.US);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void addingTheSameItemAgainResetsQuantityToOne() {
        stubItem("EST-1", "16.50", "10.00");
        service.add(cart, "EST-1", Locale.US);
        cart.updateQuantity("EST-1", 5);

        service.add(cart, "EST-1", Locale.US);

        assertThat(cart.contents()).containsEntry("EST-1", 1);
    }

    @Test
    void zeroOrGarbageQuantityRemovesTheLine() {
        cart.add("EST-1");
        cart.add("EST-2");

        service.updateQuantities(cart, Map.of("EST-1", "0", "EST-2", "lots"));

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void quantityUpdateCannotSmuggleInNewItems() {
        cart.add("EST-1");

        service.updateQuantities(cart, Map.of("EST-99", "4"));

        assertThat(cart.contents()).containsOnlyKeys("EST-1");
    }

    @Test
    void absurdQuantitiesAreCapped() {
        assertThat(CartService.parseQuantity("99999999")).isEqualTo(CartService.MAX_QUANTITY);
        assertThat(CartService.parseQuantity(" 7 ")).isEqualTo(7);
        assertThat(CartService.parseQuantity(null)).isZero();
    }

    private void stubItem(String id, String listPrice, String unitCost) {
        Item item = new Item(id, "FI-SW-01", "FISH", "Angelfish", List.of("Large"), "desc", "fish3.gif",
                new BigDecimal(listPrice), new BigDecimal(unitCost));
        when(catalog.item(eq(id), any())).thenReturn(Optional.of(item));
    }
}
