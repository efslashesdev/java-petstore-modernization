package com.petstore.catalog;

import java.math.BigDecimal;
import java.util.List;

public record Item(
        String itemId,
        String productId,
        String categoryId,
        String productName,
        List<String> attributes,
        String description,
        String image,
        BigDecimal listPrice,
        BigDecimal unitCost) {

    public Item {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    /** The first attribute doubles as the display prefix, e.g. "Large Angelfish". */
    public String displayName() {
        return attributes.isEmpty() ? productName : attributes.getFirst() + " " + productName;
    }
}
