package com.petstore.customer;

/**
 * Expiry is kept as the legacy {@code MM/YYYY} string (seed data has {@code MM/YY}).
 *
 * <p>Component order matters here: the legacy constructor was (number, expiry, type) while every
 * caller passed (number, type, expiry), so edited accounts had type and expiry swapped.
 */
public record CreditCard(String number, String type, String expiry) {

    public String expiryMonth() {
        int slash = expiry == null ? -1 : expiry.indexOf('/');
        return slash < 0 ? "" : expiry.substring(0, slash);
    }

    public String expiryYear() {
        int slash = expiry == null ? -1 : expiry.indexOf('/');
        return slash < 0 ? "" : expiry.substring(slash + 1);
    }

    public String masked() {
        if (number == null || number.isBlank()) {
            return "";
        }
        String digits = number.replaceAll("[^0-9A-Za-z]", "");
        return "**** " + (digits.length() <= 4 ? digits : digits.substring(digits.length() - 4));
    }
}
