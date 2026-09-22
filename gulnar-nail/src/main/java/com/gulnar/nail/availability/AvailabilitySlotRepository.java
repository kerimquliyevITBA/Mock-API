package com.gulnar.nail.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByDayOrderBySlotTimeAsc(AvailabilityDay day);
    Optional<AvailabilitySlot> findByDayAndSlotTime(AvailabilityDay day, LocalTime time);

    @Query("select s from AvailabilitySlot s where s.day.date between :from and :to order by s.day.date, s.slotTime")
    List<AvailabilitySlot> findByDateRange(LocalDate from, LocalDate to);
}
