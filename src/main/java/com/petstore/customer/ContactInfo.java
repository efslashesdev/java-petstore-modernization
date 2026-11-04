package com.petstore.customer;

public record ContactInfo(String givenName, String familyName, Address address, String email, String telephone) {
}
