package com.petstore.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the hand-off to the order processing center (a separate application, out of scope).
 * The legacy app sent the order over JMS and printed the stack trace if that failed, after which the
 * order was simply gone. Here the order is already persisted with status PENDING before this runs, so
 * a real integration can be a poller or a change-stream consumer working off the orders collection.
 */
@Component
class OrderFulfillmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentNotifier.class);

    @EventListener
    void on(OrderPlacedEvent event) {
        PurchaseOrder order = event.order();
        log.info("Order {} placed by {}: {} line(s), total {} ({})", order.orderId(), order.userId(),
                order.lineItems().size(), order.totalPrice(), order.locale());
    }
}
