package com.petstore.order;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Shopping cart is empty");
    }
}
