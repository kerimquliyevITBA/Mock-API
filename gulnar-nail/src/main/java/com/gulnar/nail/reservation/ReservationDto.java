package com.gulnar.nail.reservation;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class ReservationDto {

    public record CreateReq(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Pattern(regexp = "^(050|051|055|070|077|099|010)$", message = "Prefiks yanlışdır") String prefix,
            @NotBlank @Pattern(regexp = "^\\d{7}$", message = "Nömrə 7 rəqəm olmalıdır") String phone,
            @NotNull Long serviceId,
            @NotNull LocalDate date,
            @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime time
    ) {}

    public record StatusReq(
            @NotBlank String code,
            @NotBlank @Pattern(regexp = "^(050|051|055|070|077|099|010)$") String prefix,
            @NotBlank @Pattern(regexp = "^\\d{7}$") String phone
    ) {}

    public record ReservationView(
            Long id,
            String code,
            String customerName,
            String phone,
            Long serviceId,
            String serviceName,
            BigDecimal price,
            LocalDate date,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime time,
            int durationMin,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime cancelledAt
    ) {
        public static ReservationView of(Reservation r) {
            return new ReservationView(r.getId(), r.getCode(), r.getCustomerName(), r.getPhone(),
                    r.getServiceId(), r.getServiceName(), r.getPriceSnapshot(),
                    r.getReservationDate(), r.getReservationTime(), r.getDurationMin(),
                    r.getStatus().name(), r.getCreatedAt(), r.getCancelledAt());
        }
    }

    public record CreateRes(
            String code,
            LocalDate date,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime time,
            String serviceName,
            BigDecimal price,
            String customerName,
            String phone,
            String status
    ) {}
}
