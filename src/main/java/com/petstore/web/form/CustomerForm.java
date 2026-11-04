package com.petstore.web.form;

import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.Profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Used by both "create account" and "edit account"; the legacy app shared one action for the two as well. */
public class CustomerForm {

    @Valid
    private ContactForm contact = new ContactForm();

    // Digits, spaces and dashes - the seed and demo cards look like 0100-001-0001.
    @NotBlank @Size(max = 30) @Pattern(regexp = "[0-9 -]+", message = "{error.cardNumber}")
    private String cardNumber;

    @NotBlank @Size(max = 30)
    private String cardType;

    @NotBlank @Pattern(regexp = "0[1-9]|1[0-2]", message = "{error.expiryMonth}")
    private String expiryMonth;

    // Two digits allowed because the seed data uses MM/YY.
    @NotBlank @Pattern(regexp = "\\d{2}|\\d{4}", message = "{error.expiryYear}")
    private String expiryYear;

    @NotBlank @Pattern(regexp = "en_US|ja_JP|zh_CN")
    private String language = "en_US";

    @NotBlank @Size(max = 10)
    private String favoriteCategory;

    private boolean myList;
    private boolean banners;

    public static CustomerForm from(Customer customer) {
        CustomerForm form = new CustomerForm();
        form.contact = ContactForm.from(customer.contactInfo());
        CreditCard card = customer.creditCard();
        if (card != null) {
            form.cardNumber = card.number();
            form.cardType = card.type();
            form.expiryMonth = card.expiryMonth();
            form.expiryYear = card.expiryYear();
        }
        Profile profile = customer.profile();
        form.language = profile.preferredLanguage();
        form.favoriteCategory = profile.favoriteCategory();
        form.myList = profile.myList();
        form.banners = profile.banners();
        return form;
    }

    public CreditCard toCreditCard() {
        return new CreditCard(cardNumber.trim(), cardType.trim(), expiryMonth + "/" + expiryYear);
    }

    public Profile toProfile() {
        return new Profile(language, favoriteCategory, myList, banners);
    }

    public ContactForm getContact() { return contact; }
    public void setContact(ContactForm contact) { this.contact = contact; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getFavoriteCategory() { return favoriteCategory; }
    public void setFavoriteCategory(String favoriteCategory) { this.favoriteCategory = favoriteCategory; }
    public boolean isMyList() { return myList; }
    public void setMyList(boolean myList) { this.myList = myList; }
    public boolean isBanners() { return banners; }
    public void setBanners(boolean banners) { this.banners = banners; }
}
