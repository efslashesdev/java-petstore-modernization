package com.petstore.customer;

import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findById(String userId);

    void save(Customer customer);

    void deleteAll();
}
