package com.petstore.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final LayoutModelInterceptor layoutModelInterceptor;

    WebConfig(LayoutModelInterceptor layoutModelInterceptor) {
        this.layoutModelInterceptor = layoutModelInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(layoutModelInterceptor).excludePathPatterns("/images/**", "/css/**", "/actuator/**");
    }
}
