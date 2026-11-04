package com.petstore.web.form;

import com.petstore.customer.ContactInfo;

import jakarta.validation.Valid;

public class OrderForm {

    @Valid
    private ContactForm billTo = new ContactForm();

    @Valid
    private ContactForm shipTo = new ContactForm();

    /** Both addresses start as the customer's own, as on the legacy billing/shipping page. */
    public static OrderForm prefilled(ContactInfo contact) {
        OrderForm form = new OrderForm();
        form.billTo = ContactForm.from(contact);
        form.shipTo = ContactForm.from(contact);
        return form;
    }

    public ContactForm getBillTo() { return billTo; }
    public void setBillTo(ContactForm billTo) { this.billTo = billTo; }
    public ContactForm getShipTo() { return shipTo; }
    public void setShipTo(ContactForm shipTo) { this.shipTo = shipTo; }
}
