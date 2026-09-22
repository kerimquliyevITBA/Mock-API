package com.gulnar.nail.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AvailabilityIntervalRepository extends JpaRepository<AvailabilityInterval, Long> {

    List<AvailabilityInterval> findByDayOrderByStartTimeAsc(AvailabilityDay day);

    Optional<AvailabilityInterval> findByDayAndStartTime(AvailabilityDay day, LocalTime start);

    @Query("select i from AvailabilityInterval i " +
           "where i.day.date between :from and :to " +
           "order by i.day.date, i.startTime")
    List<AvailabilityInterval> findByDateRange(LocalDate from, LocalDate to);
}
