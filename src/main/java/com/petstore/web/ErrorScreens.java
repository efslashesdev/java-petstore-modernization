package com.petstore.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.petstore.order.EmptyCartException;
import com.petstore.signon.DuplicateAccountException;

/** The exception-mapping entries from the legacy mappings.xml. */
@ControllerAdvice
class ErrorScreens {

    @ExceptionHandler(DuplicateAccountException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String duplicateAccount(Model model) {
        return Views.render(model, Screen.DUPLICATE_ACCOUNT);
    }

    @ExceptionHandler(EmptyCartException.class)
    String emptyCart(Model model) {
        return Views.render(model, Screen.CART_EMPTY_ORDER_ERROR);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(Model model) {
        model.addAttribute("notFound", true);
        return Views.render(model, Screen.GENERAL_ERROR);
    }

    // Anything else falls through to Spring Boot's error handling and templates/error.html.
}
