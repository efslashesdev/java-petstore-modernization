package com.petstore.customer;

/**
 * In the legacy app this was six CMP entity beans linked by container-managed relationships.
 * None of the children ever existed outside their customer, so here they are value objects
 * inside one aggregate (and one MongoDB document).
 */
public record Customer(String userId, Account account, Profile profile) {

    public static final String ACTIVE = "active";

    public record Account(String status, ContactInfo contactInfo, CreditCard creditCard) {
    }

    public ContactInfo contactInfo() {
        return account.contactInfo();
    }

    public CreditCard creditCard() {
        return account.creditCard();
    }
}
