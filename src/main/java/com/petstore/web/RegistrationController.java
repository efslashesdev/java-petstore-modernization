package com.petstore.web;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Year;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.petstore.catalog.SupportedLocales;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CustomerService;
import com.petstore.web.form.ContactForm;
import com.petstore.web.form.CustomerForm;
import com.petstore.web.form.NewUserForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Two-step sign-up, as in the legacy store: user name and password on the sign-in page, then the
 * account details. Nothing is written until step two succeeds.
 */
@Controller
class RegistrationController {

    static final String PENDING_SIGNUP = "pendingSignup";

    private final CustomerService customers;
    private final SecurityContextRepository securityContextRepository;
    private final SignOnSuccessHandler signOnSuccessHandler;
    private final Clock clock;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    RegistrationController(CustomerService customers, SecurityContextRepository securityContextRepository,
                           SignOnSuccessHandler signOnSuccessHandler, Clock clock) {
        this.customers = customers;
        this.securityContextRepository = securityContextRepository;
        this.signOnSuccessHandler = signOnSuccessHandler;
        this.clock = clock;
    }

    record PendingSignup(String username, String passwordHash) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @PostMapping("/createuser.do")
    String createUser(@Valid @ModelAttribute("newUser") NewUserForm form, BindingResult errors,
                      HttpSession session, Model model) {
        if (errors.hasErrors()) {
            return Views.render(model, Screen.SIGNON);
        }
        if (customers.isUsernameTaken(form.getUsername())) {
            return Views.render(model, Screen.DUPLICATE_ACCOUNT);
        }
        session.setAttribute(PENDING_SIGNUP,
                new PendingSignup(form.getUsername(), customers.hashPassword(form.getPassword())));
        return "redirect:/create_customer.screen";
    }

    @GetMapping("/create_customer.screen")
    String customerForm(HttpSession session, Locale locale, Model model) {
        if (session.getAttribute(PENDING_SIGNUP) == null) {
            return "redirect:/signon.screen";
        }
        model.addAttribute("customerForm", demoDefaults(locale));
        return Views.render(model, Screen.CREATE_CUSTOMER);
    }

    @PostMapping("/createcustomer.do")
    String createCustomer(@Valid @ModelAttribute CustomerForm customerForm, BindingResult errors,
                          HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpSession session = request.getSession();
        if (!(session.getAttribute(PENDING_SIGNUP) instanceof PendingSignup pending)) {
            return "redirect:/signon.screen";
        }
        if (errors.hasErrors()) {
            return Views.render(model, Screen.CREATE_CUSTOMER);
        }

        customers.register(pending.username(), pending.passwordHash(), customerForm.getContact().toContactInfo(),
                customerForm.toCreditCard(), customerForm.toProfile());
        session.removeAttribute(PENDING_SIGNUP);

        signIn(pending.username(), request, response);
        signOnSuccessHandler.applySignOn(request, response, pending.username(), false);

        SavedRequest saved = requestCache.getRequest(request, response);
        requestCache.removeRequest(request, response);
        // Legacy CreateUserFlowHandler: back to where sign-in was demanded, unless that was the account page.
        if (saved == null || saved.getRedirectUrl().contains("/customer.do")) {
            return "redirect:/main.screen";
        }
        return "redirect:" + saved.getRedirectUrl();
    }

    private void signIn(String username, HttpServletRequest request, HttpServletResponse response) {
        request.changeSessionId();
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** The legacy form came pre-filled with demo values, which makes the sign-up demo a single click. */
    private CustomerForm demoDefaults(Locale locale) {
        CustomerForm form = new CustomerForm();
        ContactInfo demo = switch (locale.getLanguage()) {
            case "ja" -> new ContactInfo("九郎", "三舞", new Address("月町 1234", null, "山村市", "東京都", "10001", "日本"),
                    "aaa@bbb.ccc", "408-555-5555");
            case "zh" -> new ContactInfo("长城", "万", new Address("秦怀小区 1234", null, "南京市", "江苏省", "123456", "中国"),
                    "aaa@bbb.ccc", "025-4316910");
            default -> new ContactInfo("Duke", "BluesPrints", new Address("1234 Moon Way", null, "Sunville", "California",
                    "10001", "USA"), "aaa@bbb.ccc", "408-555-5555");
        };
        form.setContact(ContactForm.from(demo));
        form.setCardNumber("0100-001-0001");
        form.setCardType("Java(TM) Card");
        form.setExpiryMonth("01");
        form.setExpiryYear(String.valueOf(Year.now(clock).getValue() + 2));
        form.setLanguage(SupportedLocales.key(locale));
        form.setFavoriteCategory("BIRDS");
        return form;
    }
}
