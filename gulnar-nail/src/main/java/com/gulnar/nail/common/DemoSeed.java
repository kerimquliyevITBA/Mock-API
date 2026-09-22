package com.gulnar.nail.common;

import com.gulnar.nail.availability.AvailabilityDay;
import com.gulnar.nail.availability.AvailabilityDayRepository;
import com.gulnar.nail.availability.AvailabilityInterval;
import com.gulnar.nail.availability.AvailabilityIntervalRepository;
import com.gulnar.nail.service.ServiceEntity;
import com.gulnar.nail.service.ServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@Order(10)
public class DemoSeed implements CommandLineRunner {

    private final ServiceRepository services;
    private final AvailabilityDayRepository days;
    private final AvailabilityIntervalRepository intervals;
    private final boolean enabled;

    public DemoSeed(ServiceRepository services, AvailabilityDayRepository days, AvailabilityIntervalRepository intervals,
                    @Value("${app.seed.demo-data}") boolean enabled) {
        this.services = services;
        this.days = days;
        this.intervals = intervals;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) return;

        if (services.count() == 0) {
            services.save(svc("Manikür",       new BigDecimal("25"), 40));
            services.save(svc("Pedikür",       new BigDecimal("35"), 60));
            services.save(svc("Gel-lak",       new BigDecimal("30"), 60));
            services.save(svc("Dırnaq uzatma", new BigDecimal("50"), 120));
        }

        if (days.count() == 0) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 21; i++) {
                LocalDate date = today.plusDays(i);
                boolean closed = date.getDayOfWeek() == DayOfWeek.SUNDAY;
                AvailabilityDay d = new AvailabilityDay();
                d.setDate(date);
                d.setClosed(closed);
                d = days.save(d);
                if (!closed) {
                    intervals.save(interval(d, "10:00", "19:00"));
                }
            }
        }
    }

    private static ServiceEntity svc(String name, BigDecimal price, int min) {
        ServiceEntity e = new ServiceEntity();
        e.setName(name);
        e.setPrice(price);
        e.setDurationMin(min);
        e.setActive(true);
        return e;
    }

    private static AvailabilityInterval interval(AvailabilityDay day, String start, String end) {
        AvailabilityInterval iv = new AvailabilityInterval();
        iv.setDay(day);
        iv.setStartTime(LocalTime.parse(start));
        iv.setEndTime(LocalTime.parse(end));
        return iv;
    }
}
