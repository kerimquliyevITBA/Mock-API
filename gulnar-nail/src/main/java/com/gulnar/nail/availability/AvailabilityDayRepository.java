package com.gulnar.nail.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityDayRepository extends JpaRepository<AvailabilityDay, Long> {
    Optional<AvailabilityDay> findByDate(LocalDate date);
    List<AvailabilityDay> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
