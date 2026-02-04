package com.itda.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * Web MVC configuration
 * - CORS settings
 * - Static resource handler settings
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(
            org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        String uploadPath = "file:" + fileStorageProperties.getUploadDir() + "/";

        registry.addResourceHandler("/files/**")
                .addResourceLocations(uploadPath)
                .setCacheControl(
                        CacheControl.maxAge(24, TimeUnit.HOURS)
                                .cachePublic()
                                .staleWhileRevalidate(7, TimeUnit.DAYS)
                );
    }
}
