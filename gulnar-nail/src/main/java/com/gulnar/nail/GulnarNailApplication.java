package com.gulnar.nail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GulnarNailApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulnarNailApplication.class, args);
    }
}
