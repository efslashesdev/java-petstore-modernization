package com.petstore.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import com.petstore.web.form.NewUserForm;

@Controller
class SignOnController {

    @GetMapping("/signon.screen")
    String signOn(@CookieValue(name = SignOnSuccessHandler.REMEMBER_COOKIE, required = false) String rememberedUser,
                  Model model) {
        model.addAttribute("rememberedUser", rememberedUser);
        model.addAttribute("newUser", new NewUserForm());
        return Views.render(model, Screen.SIGNON);
    }

    @GetMapping("/signon_error.screen")
    String signOnError(Model model) {
        return Views.render(model, Screen.SIGNON_ERROR);
    }

    @GetMapping("/signoff.screen")
    String signedOff(Model model) {
        return Views.render(model, Screen.SIGNOFF);
    }
}
