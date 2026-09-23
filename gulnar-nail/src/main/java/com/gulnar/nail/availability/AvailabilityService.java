package com.gulnar.nail.availability;

import com.gulnar.nail.reservation.Reservation;
import com.gulnar.nail.reservation.ReservationRepository;
import com.gulnar.nail.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class AvailabilityService {

    private final AvailabilityDayRepository days;
    private final AvailabilitySlotRepository slots;
    private final ReservationRepository reservations;

    public AvailabilityService(AvailabilityDayRepository days,
                               AvailabilitySlotRepository slots,
                               ReservationRepository reservations) {
        this.days = days;
        this.slots = slots;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityDto.DayView> range(LocalDate from, LocalDate to, boolean adminView) {
        if (from == null || to == null || to.isBefore(from))
            throw new ResponseStatusException(BAD_REQUEST, "from/to tarixləri düzgün deyil");
        if (from.until(to).getDays() > 120)
            throw new ResponseStatusException(BAD_REQUEST, "Interval max 120 gündür");

        List<AvailabilityDay> ds = days.findByDateBetweenOrderByDateAsc(from, to);
        List<AvailabilitySlot> allSlots = slots.findByDateRange(from, to);
        Map<Long, List<AvailabilitySlot>> slotsByDayId = allSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getDay().getId()));
        List<Reservation> res = reservations.findByReservationDateBetweenAndStatus(from, to, ReservationStatus.ACTIVE);
        Map<LocalDate, Set<LocalTime>> takenByDate = res.stream()
                .collect(Collectors.groupingBy(Reservation::getReservationDate,
                        Collectors.mapping(Reservation::getReservationTime, Collectors.toSet())));

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<AvailabilityDto.DayView> out = new ArrayList<>();
        for (AvailabilityDay d : ds) {
            List<AvailabilitySlot> daySlots = new ArrayList<>(slotsByDayId.getOrDefault(d.getId(), List.of()));
            daySlots.sort(Comparator.comparing(AvailabilitySlot::getSlotTime));
            Set<LocalTime> taken = takenByDate.getOrDefault(d.getDate(), Set.of());

            List<AvailabilityDto.SlotView> viewSlots = new ArrayList<>();
            if (!d.isClosed()) {
                for (AvailabilitySlot s : daySlots) {
                    LocalTime t = s.getSlotTime();
                    boolean past = d.getDate().isBefore(today)
                            || (d.getDate().equals(today) && t.isBefore(nowTime));
                    boolean tkn = taken.contains(t);
                    if (adminView || !past) {
                        viewSlots.add(new AvailabilityDto.SlotView(t, tkn, past));
                    }
                }
            }
            out.add(new AvailabilityDto.DayView(d.getDate(), d.isClosed(), d.getNote(), viewSlots));
        }
        return out;
    }

    @Transactional
    public AvailabilityDay upsertDay(LocalDate date, Boolean closed, String note) {
        if (date == null) throw new ResponseStatusException(BAD_REQUEST, "Tarix tələb olunur");
        AvailabilityDay existing = days.findByDate(date).orElse(null);
        if (existing == null && date.isBefore(LocalDate.now()))
            throw new ResponseStatusException(BAD_REQUEST, "Keçmiş tarix üçün gün yaradıla bilməz");
        AvailabilityDay d = existing != null ? existing : new AvailabilityDay();
        if (existing == null) d.setDate(date);
        if (closed != null) {
            if (closed && hasActive(date))
                throw new ResponseStatusException(CONFLICT, "Bu gündə aktiv rezerv var — bağlamaq olmaz");
            d.setClosed(closed);
        }
        if (note != null) d.setNote(note.isBlank() ? null : note);
        return days.save(d);
    }

    @Transactional
    public void deleteDay(LocalDate date) {
        AvailabilityDay d = days.findByDate(date).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Gün tapılmadı"));
        if (hasActive(date)) throw new ResponseStatusException(CONFLICT, "Aktiv rezerv olan günü silmək olmaz");
        days.delete(d);
    }

    @Transactional
    public AvailabilitySlot addSlot(LocalDate date, LocalTime time) {
        if (date == null || time == null) throw new ResponseStatusException(BAD_REQUEST, "Tarix və saat tələb olunur");
        if (date.isBefore(LocalDate.now()))
            throw new ResponseStatusException(BAD_REQUEST, "Keçmiş tarixə saat əlavə edilə bilməz");
        AvailabilityDay d = days.findByDate(date).orElseGet(() -> {
            AvailabilityDay nd = new AvailabilityDay();
            nd.setDate(date);
            return days.save(nd);
        });
        if (d.isClosed())
            throw new ResponseStatusException(CONFLICT, "Bağlı günə saat əlavə edilə bilməz");
        LocalTime rounded = LocalTime.of(time.getHour(), time.getMinute());
        if (slots.findByDayAndSlotTime(d, rounded).isPresent())
            throw new ResponseStatusException(CONFLICT, "Bu saat artıq var");
        AvailabilitySlot s = new AvailabilitySlot();
        s.setDay(d);
        s.setSlotTime(rounded);
        return slots.save(s);
    }

    @Transactional
    public void removeSlot(LocalDate date, LocalTime time) {
        AvailabilityDay d = days.findByDate(date).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Gün tapılmadı"));
        AvailabilitySlot s = slots.findByDayAndSlotTime(d, time).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Saat tapılmadı"));
        if (reservations.existsByReservationDateAndReservationTimeAndStatus(date, time, ReservationStatus.ACTIVE))
            throw new ResponseStatusException(CONFLICT, "Bu saata aktiv rezerv var — əvvəlcə ləğv edin");
        slots.delete(s);
    }

    @Transactional(readOnly = true)
    public boolean isSlotOpen(LocalDate date, LocalTime time) {
        return days.findByDate(date)
                .filter(d -> !d.isClosed())
                .flatMap(d -> slots.findByDayAndSlotTime(d, time))
                .isPresent();
    }

    private boolean hasActive(LocalDate date) {
        return !reservations.findByReservationDateAndStatus(date, ReservationStatus.ACTIVE).isEmpty();
    }
}
