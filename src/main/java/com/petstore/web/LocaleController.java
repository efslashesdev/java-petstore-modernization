package com.petstore.web;

import java.net.URI;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import com.petstore.catalog.SupportedLocales;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
class LocaleController {

    private final LocaleResolver localeResolver;

    LocaleController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    /** The flag links in the banner. Like the legacy ClientStateFlowHandler, return to the page you were on. */
    @GetMapping("/changelocale.do")
    String change(@RequestParam(required = false) String locale,
                  @RequestHeader(name = "Referer", required = false) String referer,
                  HttpServletRequest request, HttpServletResponse response) {
        localeResolver.setLocale(request, response, SupportedLocales.parse(locale));
        return "redirect:" + sameSitePath(referer, request);
    }

    // Only follow the Referer back into this application, never to another host.
    static String sameSitePath(String referer, HttpServletRequest request) {
        if (referer == null) {
            return "/main.screen";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "/main.screen";
            }
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || !path.startsWith("/") || path.startsWith("//")) {
                return "/main.screen";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/main.screen";
        }
    }
}
