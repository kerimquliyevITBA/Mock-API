package com.gulnar.nail.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCodeAndPhone(String code, String phone);

    boolean existsByReservationDateAndReservationTimeAndStatus(LocalDate date, LocalTime time, ReservationStatus status);

    boolean existsByPhoneAndReservationDateAndReservationTimeAndStatus(
            String phone, LocalDate date, LocalTime time, ReservationStatus status);

    List<Reservation> findByReservationDateAndStatus(LocalDate date, ReservationStatus status);

    List<Reservation> findByReservationDateBetweenAndStatus(LocalDate from, LocalDate to, ReservationStatus status);

    List<Reservation> findByReservationDateBetweenOrderByReservationDateAscReservationTimeAsc(LocalDate from, LocalDate to);

    boolean existsByCode(String code);

    @Modifying
    @Query("update Reservation r set r.status = com.gulnar.nail.reservation.ReservationStatus.COMPLETED, r.updatedAt = CURRENT_TIMESTAMP " +
           "where r.status = com.gulnar.nail.reservation.ReservationStatus.ACTIVE " +
           "and (r.reservationDate < :today or (r.reservationDate = :today and r.reservationTime < :now))")
    int completeExpired(@Param("today") LocalDate today, @Param("now") LocalTime now);

    @Query("select count(r) from Reservation r where r.reservationDate = :d and r.status = com.gulnar.nail.reservation.ReservationStatus.ACTIVE")
    long countActiveOn(@Param("d") LocalDate date);

    @Query("select count(r) from Reservation r where r.reservationDate >= :d and r.status = com.gulnar.nail.reservation.ReservationStatus.ACTIVE")
    long countActiveFrom(@Param("d") LocalDate date);
}
