package com.gulnar.nail.service;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ServiceDto {

    public record ServiceView(Long id, String name, BigDecimal price, int durationMin, boolean active) {
        public static ServiceView of(ServiceEntity e) {
            return new ServiceView(e.getId(), e.getName(), e.getPrice(), e.getDurationMin(), e.isActive());
        }
    }

    public record UpsertReq(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @NotNull @Min(20) @Max(600) Integer durationMin,
            Boolean active
    ) {
        @jakarta.validation.constraints.AssertTrue(message = "Müddət 20 dəqiqənin misli olmalıdır")
        public boolean isStepOf20() { return durationMin != null && durationMin % 20 == 0; }
    }
}
