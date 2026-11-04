package com.petstore.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.petstore.web.form.CustomerForm;

class CreditCardTest {

    @Test
    void formKeepsCardTypeAndExpiryInTheirOwnFields() {
        // Regression: legacy CustomerHTMLAction called new CreditCard(number, type, expiry) against a
        // (number, expiry, type) constructor, so edited accounts had the two swapped.
        CustomerForm form = new CustomerForm();
        form.setCardNumber("0100-001-0001");
        form.setCardType("Duke Express");
        form.setExpiryMonth("07");
        form.setExpiryYear("2029");

        CreditCard card = form.toCreditCard();

        assertThat(card.type()).isEqualTo("Duke Express");
        assertThat(card.expiry()).isEqualTo("07/2029");
        assertThat(card.expiryMonth()).isEqualTo("07");
        assertThat(card.expiryYear()).isEqualTo("2029");
    }

    @Test
    void parsesTheShortSeedFormat() {
        CreditCard seed = new CreditCard("123456789", "Meow Card", "04/04");

        assertThat(seed.expiryMonth()).isEqualTo("04");
        assertThat(seed.expiryYear()).isEqualTo("04");
    }

    @Test
    void expiryWithoutSlashYieldsEmptyParts() {
        CreditCard card = new CreditCard("1", "x", "garbage");

        assertThat(card.expiryMonth()).isEmpty();
        assertThat(card.expiryYear()).isEmpty();
    }

    @Test
    void maskShowsOnlyTheLastFourCharacters() {
        assertThat(new CreditCard("0100-001-0001", "x", "01/30").masked()).isEqualTo("**** 0001");
        assertThat(new CreditCard("123", "x", "01/30").masked()).isEqualTo("**** 123");
        assertThat(new CreditCard(null, "x", "01/30").masked()).isEmpty();
    }
}
