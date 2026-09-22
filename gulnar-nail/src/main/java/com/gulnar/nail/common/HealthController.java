package com.gulnar.nail.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "service", "gulnar-nail", "time", OffsetDateTime.now().toString());
    }
}
