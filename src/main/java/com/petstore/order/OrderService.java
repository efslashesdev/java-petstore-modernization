package com.petstore.order;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.petstore.cart.CartService;
import com.petstore.cart.CartService.CartLine;
import com.petstore.cart.CartService.CartView;
import com.petstore.cart.ShoppingCart;
import com.petstore.catalog.SupportedLocales;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;

@Service
public class OrderService {

    static final String ORDER_ID_PREFIX = "1001";

    private final CartService cartService;
    private final OrderRepository orders;
    private final OrderIdGenerator ids;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderService(CartService cartService, OrderRepository orders, OrderIdGenerator ids,
                        ApplicationEventPublisher events, Clock clock) {
        this.cartService = cartService;
        this.orders = orders;
        this.ids = ids;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Prices are taken from the catalog at the moment of ordering, in the shopper's locale, never from
     * anything the browser sent. The legacy action charged a hard-coded test card; we charge the card
     * on the customer's account.
     */
    public PurchaseOrder placeOrder(String userId, ContactInfo billTo, ContactInfo shipTo, CreditCard card,
                                    ShoppingCart cart, Locale locale) {
        CartView view = cartService.view(cart, locale);
        if (view.isEmpty()) {
            throw new EmptyCartException();
        }

        List<PurchaseOrder.LineItem> lineItems = new ArrayList<>();
        for (CartLine line : view.lines()) {
            var item = line.item();
            lineItems.add(new PurchaseOrder.LineItem(lineItems.size(), item.categoryId(), item.productId(),
                    item.itemId(), line.quantity(), line.unitPrice()));
        }

        PurchaseOrder order = new PurchaseOrder(
                ids.nextId(ORDER_ID_PREFIX),
                userId,
                billTo.email(),
                clock.instant(),
                SupportedLocales.key(locale),
                billTo,
                shipTo,
                new PurchaseOrder.PaymentCard(card.type(), card.masked(), card.expiry()),
                lineItems,
                view.subTotal(),
                PurchaseOrder.Status.PENDING);

        orders.insert(order);
        cart.clear();
        events.publishEvent(new OrderPlacedEvent(order));
        return order;
    }
}
