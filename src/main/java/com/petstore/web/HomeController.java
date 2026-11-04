package com.petstore.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import com.petstore.config.PetstoreProperties;
import com.petstore.populate.DataPopulator;

@Controller
class HomeController {

    private final DataPopulator populator;
    private final PetstoreProperties properties;

    HomeController(DataPopulator populator, PetstoreProperties properties) {
        this.populator = populator;
        this.properties = properties;
    }

    @GetMapping("/")
    String index(Model model) {
        model.addAttribute("allowRepopulate", properties.populate().allowForce());
        return "index";
    }

    /** The legacy "forcefully repopulate" link, now a POST and switchable off. */
    @PostMapping("/populate")
    String repopulate() {
        if (!properties.populate().allowForce()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        populator.populate(true);
        return "redirect:/main.screen";
    }
}
