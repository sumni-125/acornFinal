package com.example.ocean.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // favicon 관련 요청을 정적 리소스로 처리
        registry.addResourceHandler("/favicon.ico", "/favicon-*.png", "/favicon.svg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}