package com.petstore.web.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void contactRequiresTheSameFieldsAsLegacy() {
        ContactForm form = new ContactForm();

        assertThat(invalidFields(validator.validate(form)))
                .containsExactlyInAnyOrder("givenName", "familyName", "street1", "city", "state", "zipCode", "telephone");
    }

    @Test
    void emailIsOptionalButMustBeAnAddressWhenGiven() {
        ContactForm form = validContact();
        form.setEmail("");
        assertThat(validator.validate(form)).isEmpty();

        form.setEmail("not-an-email");
        assertThat(invalidFields(validator.validate(form))).containsExactly("email");
    }

    @Test
    void lengthsFromTheLegacyMaxlengthAttributesAreEnforcedServerSide() {
        ContactForm form = validContact();
        form.setZipCode("1234567890123");

        assertThat(invalidFields(validator.validate(form))).containsExactly("zipCode");
    }

    @Test
    void newUserRulesComeFromUserEjb() {
        NewUserForm form = newUser("bad*name", "secret", "secret");
        assertThat(invalidFields(validator.validate(form))).containsExactly("username");

        form = newUser("a".repeat(26), "secret", "secret");
        assertThat(invalidFields(validator.validate(form))).containsExactly("username");
    }

    @Test
    void passwordRepeatMustMatch() {
        assertThat(invalidFields(validator.validate(newUser("duke", "secret", "secreT"))))
                .containsExactly("passwordConfirmed");
    }

    @Test
    void storedValuesOutsideTheDropDownStaySelectable() {
        FormOptions options = new FormOptions(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        assertThat(options.states(java.util.Locale.US, "CA")).first()
                .isEqualTo(new FormOptions.Option("CA", "CA"));
        assertThat(options.years("04")).first().isEqualTo(new FormOptions.Option("04", "04"));
        assertThat(options.years(null)).first().isEqualTo(new FormOptions.Option("2026", "2026"));
    }

    private static ContactForm validContact() {
        ContactForm form = new ContactForm();
        form.setGivenName("Duke");
        form.setFamilyName("Blueprints");
        form.setStreet1("1 Moon Way");
        form.setCity("Sunville");
        form.setState("California");
        form.setZipCode("10001");
        form.setTelephone("555-0100");
        return form;
    }

    private static NewUserForm newUser(String username, String password, String repeat) {
        NewUserForm form = new NewUserForm();
        form.setUsername(username);
        form.setPassword(password);
        form.setPasswordRepeat(repeat);
        return form;
    }

    private static Set<String> invalidFields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
