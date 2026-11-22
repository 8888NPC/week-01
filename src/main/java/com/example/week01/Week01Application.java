package com.example.week01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Week01Application {
    public static void main(String[] args) {
        SpringApplication.run(Week01Application.class, args);
    }
}
