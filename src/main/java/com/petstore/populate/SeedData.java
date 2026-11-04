package com.petstore.populate;

import java.util.List;

import com.petstore.catalog.CatalogData;
import com.petstore.customer.Customer;

public record SeedData(List<SeedUser> users, List<Customer> customers, CatalogData.Snapshot catalog) {

    public record SeedUser(String username, String password) {
    }
}
