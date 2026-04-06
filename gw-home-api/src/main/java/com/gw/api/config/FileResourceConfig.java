package com.gw.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    private final FileUploadProperties fileUploadProperties;

    public FileResourceConfig(FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = fileUploadProperties.getPath();

        if (uploadPath == null || uploadPath.isBlank()) {
            return;
        }

        String normalizedUploadPath = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + normalizedUploadPath);
    }
}
