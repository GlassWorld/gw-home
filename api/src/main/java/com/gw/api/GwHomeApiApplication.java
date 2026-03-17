package com.gw.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gw")
public class GwHomeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GwHomeApiApplication.class, args);
    }
}
