package com.gulnar.nail.scheduler;

import com.gulnar.nail.reservation.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class ExpiredReservationJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredReservationJob.class);
    private final ReservationRepository repo;

    public ExpiredReservationJob(ReservationRepository repo) { this.repo = repo; }

    // Every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void run() {
        int updated = repo.completeExpired(LocalDate.now(), LocalTime.now());
        if (updated > 0) log.info("[cron] marked {} expired reservations as COMPLETED", updated);
    }
}
