package com.gulnar.nail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GulnarNailApplication {

    private static final Logger log = LoggerFactory.getLogger(GulnarNailApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GulnarNailApplication.class, args);
    }

    @Bean
    @Order(1000)
    CommandLineRunner banner(Environment env,
                             @Value("${server.port:8080}") String port,
                             @Value("${app.cors.allowed-origins}") String origins) {
        return args -> {
            String schema = env.getProperty("DB_SCHEMA", "gulnar");
            log.info("==========================================================");
            log.info("  Gulnar Nail API is up");
            log.info("  Schema:     {}", schema);
            log.info("  Port:       {}", port);
            log.info("  CORS:       {}", origins);
            log.info("  Endpoints:");
            log.info("    GET  /api/v1/health");
            log.info("    GET  /api/v1/services");
            log.info("    GET  /api/v1/availability?from=&to=");
            log.info("    POST /api/v1/reservations");
            log.info("    POST /api/v1/reservations/status");
            log.info("    POST /api/v1/auth/login");
            log.info("    ... /api/v1/admin/**   (ROLE_ADMIN)");
            log.info("==========================================================");
        };
    }
}
