package com.petstore.web;

import org.springframework.ui.Model;

final class Views {

    private Views() {
    }

    static String render(Model model, Screen screen) {
        model.addAttribute(Screen.MODEL_KEY, screen);
        return Screen.LAYOUT;
    }
}
