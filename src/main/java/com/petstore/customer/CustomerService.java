package com.petstore.customer;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.petstore.signon.UserAccount;
import com.petstore.signon.UserRepository;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customers;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customers, UserRepository users, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Customer> find(String userId) {
        return userId == null ? Optional.empty() : customers.findById(userId);
    }

    public boolean isUsernameTaken(String username) {
        return users.exists(username);
    }

    /** Lets the web tier hold a hash, never the raw password, between the two sign-up steps. */
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * The legacy flow created the login on page one of sign-up and the customer on page two, so anyone
     * who abandoned the form left a login that crashed on sign-in. We write both at the end instead.
     * Users and customers live in separate collections, so a failed customer write is compensated.
     */
    public Customer register(String username, String passwordHash, ContactInfo contactInfo,
                             CreditCard card, Profile profile) {
        users.insert(new UserAccount(username, passwordHash));
        Customer customer = new Customer(username, new Customer.Account(Customer.ACTIVE, contactInfo, card), profile);
        try {
            customers.save(customer);
        } catch (RuntimeException e) {
            users.delete(username);
            throw e;
        }
        log.info("Registered customer {}", username);
        return customer;
    }

    public Customer update(String userId, ContactInfo contactInfo, CreditCard card, Profile profile) {
        Customer existing = customers.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No customer record for " + userId));
        Customer updated = new Customer(userId,
                new Customer.Account(existing.account().status(), contactInfo, card), profile);
        customers.save(updated);
        return updated;
    }
}
