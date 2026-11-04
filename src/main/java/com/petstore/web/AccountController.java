package com.petstore.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.LocaleResolver;

import com.petstore.catalog.SupportedLocales;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerService;
import com.petstore.web.form.CustomerForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
class AccountController {

    private final CustomerService customers;
    private final CurrentCustomer currentCustomer;
    private final LocaleResolver localeResolver;

    AccountController(CustomerService customers, CurrentCustomer currentCustomer, LocaleResolver localeResolver) {
        this.customers = customers;
        this.currentCustomer = currentCustomer;
        this.localeResolver = localeResolver;
    }

    @GetMapping("/customer.do")
    String show(Model model) {
        model.addAttribute("customer", currentCustomer.require());
        return Views.render(model, Screen.CUSTOMER);
    }

    @GetMapping("/update_customer.screen")
    String edit(Model model) {
        model.addAttribute("customerForm", CustomerForm.from(currentCustomer.require()));
        return Views.render(model, Screen.UPDATE_CUSTOMER);
    }

    @PostMapping(value = "/customer.do", params = "action=update")
    String update(@Valid @ModelAttribute CustomerForm customerForm, BindingResult errors,
                  HttpServletRequest request, HttpServletResponse response, Model model) {
        if (errors.hasErrors()) {
            return Views.render(model, Screen.UPDATE_CUSTOMER);
        }
        Customer updated = customers.update(currentCustomer.require().userId(),
                customerForm.getContact().toContactInfo(), customerForm.toCreditCard(), customerForm.toProfile());
        // Legacy CustomerEJBAction also switched the UI to the newly chosen language.
        localeResolver.setLocale(request, response, SupportedLocales.parse(updated.profile().preferredLanguage()));
        return "redirect:/customer.do";
    }
}
