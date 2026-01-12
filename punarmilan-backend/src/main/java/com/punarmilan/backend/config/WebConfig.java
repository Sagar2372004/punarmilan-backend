package com.punarmilan.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Windows मध्ये file: prefix सोबत path
        String uploadPath = "file:" + uploadDir + "/";
        
        log.info("Configuring static resources from: {}", uploadPath);
        log.info("Upload directory absolute path: {}", 
            new java.io.File(uploadDir).getAbsolutePath());
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600);
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}