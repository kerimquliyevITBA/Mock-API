package com.gulnar.nail.common;

import com.gulnar.nail.availability.AvailabilityDay;
import com.gulnar.nail.availability.AvailabilityDayRepository;
import com.gulnar.nail.availability.AvailabilitySlot;
import com.gulnar.nail.availability.AvailabilitySlotRepository;
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
import java.util.List;
import java.util.Map;

@Component
@Order(10)
public class DemoSeed implements CommandLineRunner {

    private final ServiceRepository services;
    private final AvailabilityDayRepository days;
    private final AvailabilitySlotRepository slots;
    private final boolean enabled;

    public DemoSeed(ServiceRepository services, AvailabilityDayRepository days, AvailabilitySlotRepository slots,
                    @Value("${app.seed.demo-data}") boolean enabled) {
        this.services = services;
        this.days = days;
        this.slots = slots;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) return;

        if (services.count() == 0) {
            services.save(svc("Manikür",       new BigDecimal("25"), 45));
            services.save(svc("Pedikür",       new BigDecimal("35"), 60));
            services.save(svc("Gel-lak",       new BigDecimal("30"), 60));
            services.save(svc("Dırnaq uzatma", new BigDecimal("50"), 90));
        }

        if (days.count() == 0) {
            Map<DayOfWeek, List<String>> weekly = Map.of(
                    DayOfWeek.MONDAY,    List.of("11:00","12:00","14:00","15:00","16:00","17:00"),
                    DayOfWeek.TUESDAY,   List.of("10:00","11:00","13:00","14:00","18:00","19:00"),
                    DayOfWeek.WEDNESDAY, List.of("11:00","12:00","13:00","16:00","17:00"),
                    DayOfWeek.THURSDAY,  List.of("10:00","14:00","15:00","16:00","17:00","18:00"),
                    DayOfWeek.FRIDAY,    List.of("11:00","12:00","13:00","14:00","15:00","18:00","19:00"),
                    DayOfWeek.SATURDAY,  List.of("10:00","11:00","12:00","13:00","14:00")
            );

            LocalDate today = LocalDate.now();
            for (int i = 0; i < 21; i++) {
                LocalDate date = today.plusDays(i);
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    AvailabilityDay d = new AvailabilityDay();
                    d.setDate(date);
                    d.setClosed(true);
                    days.save(d);
                    continue;
                }
                List<String> times = weekly.get(date.getDayOfWeek());
                if (times == null) continue;
                AvailabilityDay d = new AvailabilityDay();
                d.setDate(date);
                d = days.save(d);
                for (String t : times) {
                    AvailabilitySlot s = new AvailabilitySlot();
                    s.setDay(d);
                    s.setSlotTime(LocalTime.parse(t));
                    slots.save(s);
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
}
