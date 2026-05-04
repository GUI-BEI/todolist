package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    // 头像存储路径（项目根目录下的 avatars 文件夹）
    private static final String AVATAR_PATH = System.getProperty("user.dir") + "/avatars/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /avatars/** 到本地文件夹
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + AVATAR_PATH);
    }

    public static String getAvatarPath() {
        return AVATAR_PATH;
    }
}