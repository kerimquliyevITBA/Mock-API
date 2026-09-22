package com.gulnar.nail.common;

import com.gulnar.nail.availability.AvailabilitySlotRepository;
import com.gulnar.nail.reservation.ReservationRepository;
import com.gulnar.nail.reservation.ReservationStatus;
import org.springframework.transaction.annotation.Transactional;
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
    private final AvailabilitySlotRepository slots;

    public AdminStatsController(ReservationRepository reservations, AvailabilitySlotRepository slots) {
        this.reservations = reservations;
        this.slots = slots;
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);
        LocalTime now = LocalTime.now();

        long todayCount = reservations.findByReservationDateAndStatus(today, ReservationStatus.ACTIVE).stream()
                .filter(r -> !r.getReservationTime().isBefore(now))
                .count();
        long upcoming = reservations.countActiveFrom(today);

        long weekOpen = slots.findByDateRange(today, weekEnd).size();
        long weekActive = reservations.findByReservationDateBetweenAndStatus(today, weekEnd, ReservationStatus.ACTIVE).size();
        long weekFree = Math.max(0, weekOpen - weekActive);

        return Map.of(
                "activeUpcoming", upcoming,
                "todayActive", todayCount,
                "weekFreeSlots", weekFree
        );
    }
}
