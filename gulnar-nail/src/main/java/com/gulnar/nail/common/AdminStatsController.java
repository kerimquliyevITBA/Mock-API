package com.gulnar.nail.common;

import com.gulnar.nail.availability.AvailabilityInterval;
import com.gulnar.nail.availability.AvailabilityIntervalRepository;
import com.gulnar.nail.reservation.Reservation;
import com.gulnar.nail.reservation.ReservationRepository;
import com.gulnar.nail.reservation.ReservationStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminStatsController {

    private static final int STEP_MIN = 20;

    private final ReservationRepository reservations;
    private final AvailabilityIntervalRepository intervals;

    public AdminStatsController(ReservationRepository reservations, AvailabilityIntervalRepository intervals) {
        this.reservations = reservations;
        this.intervals = intervals;
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

        // Rough "free" measure: total interval minutes minus booked minutes,
        // expressed as 20-minute buckets. Tells the admin at a glance how
        // much of the week is still bookable, without over-counting the
        // handful of edge slots that don't fit their real service durations.
        List<AvailabilityInterval> weekIntervals = intervals.findByDateRange(today, weekEnd);
        long totalOpenBuckets = weekIntervals.stream()
                .mapToLong(iv -> iv.durationMinutes() / STEP_MIN)
                .sum();
        long bookedBuckets = reservations.findByReservationDateBetweenAndStatus(today, weekEnd, ReservationStatus.ACTIVE).stream()
                .mapToLong(r -> Math.max(1L, r.getDurationMin() / STEP_MIN))
                .sum();
        long weekFree = Math.max(0, totalOpenBuckets - bookedBuckets);

        return Map.of(
                "activeUpcoming", upcoming,
                "todayActive", todayCount,
                "weekFreeSlots", weekFree
        );
    }
}
