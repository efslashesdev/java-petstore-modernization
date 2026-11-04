package com.petstore.web;

import java.io.IOException;
import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import com.petstore.catalog.SupportedLocales;
import com.petstore.customer.CustomerService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * What the legacy SignOnFilter + SignOnNotifier did after a good password: remember (or forget) the user
 * name, switch to the customer's preferred language, and go back to the page that asked for sign-in.
 */
@Component
public class SignOnSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    static final String REMEMBER_COOKIE = "bp_signon";
    static final String REMEMBER_PARAM = "j_remember_username";

    private final CustomerService customers;
    private final LocaleResolver localeResolver;

    SignOnSuccessHandler(CustomerService customers, LocaleResolver localeResolver) {
        this.customers = customers;
        this.localeResolver = localeResolver;
        setDefaultTargetUrl("/main.screen");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        applySignOn(request, response, authentication.getName(), request.getParameter(REMEMBER_PARAM) != null);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    void applySignOn(HttpServletRequest request, HttpServletResponse response, String username, boolean remember) {
        // Same 31-day lifetime as the legacy cookie; HttpOnly is new.
        ResponseCookie cookie = ResponseCookie.from(REMEMBER_COOKIE, remember ? username : "")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(remember ? Duration.ofDays(31) : Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        customers.find(username).ifPresent(customer -> localeResolver.setLocale(request, response,
                SupportedLocales.parse(customer.profile().preferredLanguage())));
    }
}
