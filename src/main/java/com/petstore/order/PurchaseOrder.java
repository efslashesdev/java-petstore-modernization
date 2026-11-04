package com.petstore.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.petstore.customer.ContactInfo;

/**
 * What the legacy app serialized to XML and dropped on the OPC JMS queue. It is now stored first,
 * then announced with {@link OrderPlacedEvent}.
 */
public record PurchaseOrder(
        String orderId,
        String userId,
        String email,
        Instant orderDate,
        String locale,
        ContactInfo billTo,
        ContactInfo shipTo,
        PaymentCard card,
        List<LineItem> lineItems,
        BigDecimal totalPrice,
        Status status) {

    public enum Status { PENDING }

    public record LineItem(int lineNumber, String categoryId, String productId, String itemId,
                           int quantity, BigDecimal unitPrice) {
    }

    /** Never the full card number - only enough to show the customer which card was charged. */
    public record PaymentCard(String type, String maskedNumber, String expiry) {
    }
}
