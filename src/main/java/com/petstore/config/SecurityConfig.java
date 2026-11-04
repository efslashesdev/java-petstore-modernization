package com.petstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.petstore.web.SignOnSuccessHandler;

/**
 * Replaces the legacy SignOnFilter and signon-config.xml.
 *
 * <p>Legacy only protected {@code customer.do} and {@code signon_welcome.screen}; checkout was open, so an
 * anonymous shopper could place an order with a null user id. Checkout and the account edit page now
 * require a signed-in customer as well.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SignOnSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/customer.do",
                                "/update_customer.screen",
                                "/signon_welcome.screen",
                                "/enter_order_information.screen",
                                "/order.do",
                                "/order_complete.screen").authenticated()
                        .anyRequest().permitAll())
                // Field and URL names kept from the legacy form so the sign-in markup did not change.
                .formLogin(form -> form
                        .loginPage("/signon.screen")
                        .loginProcessingUrl("/j_signon_check")
                        .usernameParameter("j_username")
                        .passwordParameter("j_password")
                        .successHandler(successHandler)
                        .failureUrl("/signon_error.screen")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/signoff.do")
                        .logoutSuccessUrl("/signoff.screen"))
                .securityContext(context -> context.securityContextRepository(securityContextRepository()));
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
