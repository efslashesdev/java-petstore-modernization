package com.petstore.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.petstore.catalog.SupportedLocales;

@Configuration
@EnableCaching
class AppConfig {

    static final String LOCALE_COOKIE = "petstore_locale";

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * The legacy store kept the locale in the session and copied it across the sign-out session reset by
     * hand. A cookie survives sign-out on its own. Like the legacy servlet's default_locale init-param,
     * we default to en_US and ignore Accept-Language.
     */
    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE);
        resolver.setDefaultLocale(SupportedLocales.DEFAULT);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }
}
