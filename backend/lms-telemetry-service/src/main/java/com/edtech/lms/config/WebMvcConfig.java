package com.edtech.lms.config;

import com.edtech.lms.security.SecurityContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityContextInterceptor securityContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply to all API endpoints
        registry.addInterceptor(securityContextInterceptor)
                .addPathPatterns("/api/**")
                // Exclude any internal paths if necessary
                .excludePathPatterns("/api/public/**");
    }
}
