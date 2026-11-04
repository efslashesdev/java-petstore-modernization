package com.petstore.web;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.petstore.customer.Customer;
import com.petstore.customer.CustomerService;

/** Loads the signed-in customer at most once per request, however many fragments ask for it. */
@Component
@RequestScope
public class CurrentCustomer {

    private final CustomerService customers;
    private Optional<Customer> customer;

    public CurrentCustomer(CustomerService customers) {
        this.customers = customers;
    }

    public Optional<String> userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }

    public boolean isSignedOn() {
        return userId().isPresent();
    }

    public Optional<Customer> get() {
        if (customer == null) {
            customer = userId().flatMap(customers::find);
        }
        return customer;
    }

    public Customer require() {
        return get().orElseThrow(() -> new IllegalStateException("Signed-in user has no customer record"));
    }
}
