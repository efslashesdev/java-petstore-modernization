package com.petstore.web.form;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Required fields are the ones the legacy extractContactInfo() checked. Lengths are the maxlength
 * attributes from the legacy forms, which were only ever enforced by the browser.
 */
public class ContactForm {

    @NotBlank @Size(max = 30)
    private String givenName;

    @NotBlank @Size(max = 30)
    private String familyName;

    @NotBlank @Size(max = 70)
    private String street1;

    @Size(max = 70)
    private String street2;

    @NotBlank @Size(max = 70)
    private String city;

    @NotBlank @Size(max = 70)
    private String state;

    @NotBlank @Size(max = 12)
    private String zipCode;

    @Size(max = 70)
    private String country;

    @NotBlank @Size(max = 70)
    private String telephone;

    // Optional in the legacy form, but if given it ends up on the order, so it has to look like an address.
    @Email @Size(max = 70)
    private String email;

    public static ContactForm from(ContactInfo info) {
        ContactForm form = new ContactForm();
        if (info == null) {
            return form;
        }
        form.givenName = info.givenName();
        form.familyName = info.familyName();
        form.email = info.email();
        form.telephone = info.telephone();
        Address a = info.address();
        if (a != null) {
            form.street1 = a.street1();
            form.street2 = a.street2();
            form.city = a.city();
            form.state = a.state();
            form.zipCode = a.zipCode();
            form.country = a.country();
        }
        return form;
    }

    public ContactInfo toContactInfo() {
        return new ContactInfo(trim(givenName), trim(familyName),
                new Address(trim(street1), blankToNull(street2), trim(city), trim(state), trim(zipCode), trim(country)),
                blankToNull(email), trim(telephone));
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getGivenName() { return givenName; }
    public void setGivenName(String givenName) { this.givenName = givenName; }
    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public String getStreet1() { return street1; }
    public void setStreet1(String street1) { this.street1 = street1; }
    public String getStreet2() { return street2; }
    public void setStreet2(String street2) { this.street2 = street2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
