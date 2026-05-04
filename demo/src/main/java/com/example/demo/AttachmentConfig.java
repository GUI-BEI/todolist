package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AttachmentConfig implements WebMvcConfigurer {

    private static final String ATTACHMENT_PATH = System.getProperty("user.dir") + "/attachments/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/attachments/**")
                .addResourceLocations("file:" + ATTACHMENT_PATH);
    }

    public static String getAttachmentPath() {
        return ATTACHMENT_PATH;
    }
}