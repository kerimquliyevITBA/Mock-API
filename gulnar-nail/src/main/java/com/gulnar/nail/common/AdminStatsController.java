package com.gulnar.nail.common;

import com.gulnar.nail.availability.AvailabilityDayRepository;
import com.gulnar.nail.reservation.ReservationRepository;
import com.gulnar.nail.reservation.ReservationStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminStatsController {

    private final ReservationRepository reservations;
    private final AvailabilityDayRepository days;

    public AdminStatsController(ReservationRepository reservations, AvailabilityDayRepository days) {
        this.reservations = reservations;
        this.days = days;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);

        long todayCount = reservations.findByReservationDateAndStatus(today, ReservationStatus.ACTIVE).stream()
                .filter(r -> r.getReservationTime().isAfter(LocalTime.now().minusMinutes(1)))
                .count();
        long upcoming = reservations.countActiveFrom(today);

        long weekOpen = days.findByDateBetweenOrderByDateAsc(today, weekEnd).stream()
                .filter(d -> !d.isClosed())
                .mapToLong(d -> d.getSlots().size())
                .sum();
        long weekActive = reservations.findByReservationDateBetweenAndStatus(today, weekEnd, ReservationStatus.ACTIVE).size();
        long weekFree = Math.max(0, weekOpen - weekActive);

        return Map.of(
                "activeUpcoming", upcoming,
                "todayActive", todayCount,
                "weekFreeSlots", weekFree
        );
    }
}
