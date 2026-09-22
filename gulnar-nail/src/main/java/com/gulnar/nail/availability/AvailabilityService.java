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

    public static final int STEP_MIN = 20;

    private final AvailabilityDayRepository days;
    private final AvailabilityIntervalRepository intervals;
    private final ReservationRepository reservations;

    public AvailabilityService(AvailabilityDayRepository days,
                               AvailabilityIntervalRepository intervals,
                               ReservationRepository reservations) {
        this.days = days;
        this.intervals = intervals;
        this.reservations = reservations;
    }

    /**
     * Returns days in range with their working intervals and generated slot
     * start times. When durationMin is null, defaults to STEP_MIN so the
     * caller sees the raw grid; a real booking flow should pass the selected
     * service's durationMin so the slots reflect what actually fits.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityDto.DayView> range(LocalDate from, LocalDate to, boolean adminView, Integer durationMin) {
        if (from == null || to == null || to.isBefore(from))
            throw new ResponseStatusException(BAD_REQUEST, "from/to tarixləri düzgün deyil");
        if (from.until(to).getDays() > 120)
            throw new ResponseStatusException(BAD_REQUEST, "Interval max 120 gündür");

        int duration = durationMin == null || durationMin <= 0 ? STEP_MIN : durationMin;
        if (duration % STEP_MIN != 0)
            throw new ResponseStatusException(BAD_REQUEST, "Xidmət müddəti " + STEP_MIN + " dəqiqənin misli olmalıdır");

        List<AvailabilityDay> ds = days.findByDateBetweenOrderByDateAsc(from, to);
        List<Reservation> res = reservations.findByReservationDateBetweenAndStatus(from, to, ReservationStatus.ACTIVE);
        Map<LocalDate, List<Reservation>> resByDate = res.stream()
                .collect(Collectors.groupingBy(Reservation::getReservationDate));

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<AvailabilityDto.DayView> out = new ArrayList<>();
        for (AvailabilityDay d : ds) {
            List<AvailabilityInterval> is = new ArrayList<>(d.getIntervals());
            is.sort(Comparator.comparing(AvailabilityInterval::getStartTime));
            List<AvailabilityDto.IntervalView> intervalViews = is.stream()
                    .map(i -> new AvailabilityDto.IntervalView(i.getId(), i.getStartTime(), i.getEndTime()))
                    .toList();

            List<Reservation> dayRes = resByDate.getOrDefault(d.getDate(), List.of());

            List<AvailabilityDto.SlotView> slots = new ArrayList<>();
            if (!d.isClosed()) {
                for (AvailabilityInterval iv : is) {
                    LocalTime t = iv.getStartTime();
                    while (!t.plusMinutes(duration).isAfter(iv.getEndTime())) {
                        boolean past = d.getDate().isBefore(today)
                                || (d.getDate().equals(today) && t.isBefore(nowTime));
                        boolean taken = overlapsAny(dayRes, t, duration);
                        if (adminView || !past) {
                            slots.add(new AvailabilityDto.SlotView(t, taken, past));
                        }
                        t = t.plusMinutes(STEP_MIN);
                    }
                }
            }

            out.add(new AvailabilityDto.DayView(
                    d.getDate(), d.isClosed(), d.getNote(),
                    intervalViews, slots
            ));
        }
        return out;
    }

    private static boolean overlapsAny(List<Reservation> dayRes, LocalTime start, int durationMin) {
        LocalTime end = start.plusMinutes(durationMin);
        for (Reservation r : dayRes) {
            LocalTime rStart = r.getReservationTime();
            LocalTime rEnd = rStart.plusMinutes(r.getDurationMin());
            if (start.isBefore(rEnd) && rStart.isBefore(end)) return true;
        }
        return false;
    }

    @Transactional
    public AvailabilityDay upsertDay(LocalDate date, Boolean closed, String note) {
        if (date == null) throw new ResponseStatusException(BAD_REQUEST, "Tarix tələb olunur");
        AvailabilityDay d = days.findByDate(date).orElseGet(() -> {
            AvailabilityDay nd = new AvailabilityDay();
            nd.setDate(date);
            return nd;
        });
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
    public AvailabilityInterval addInterval(LocalDate date, LocalTime start, LocalTime end) {
        if (date == null || start == null || end == null)
            throw new ResponseStatusException(BAD_REQUEST, "Tarix və aralıq tələb olunur");
        if (!end.isAfter(start))
            throw new ResponseStatusException(BAD_REQUEST, "Bitiş saatı başlanğıcdan sonra olmalıdır");
        if (start.getMinute() % STEP_MIN != 0 || end.getMinute() % STEP_MIN != 0)
            throw new ResponseStatusException(BAD_REQUEST, "Saatlar " + STEP_MIN + " dəqiqənin misli olmalıdır");
        if (((end.toSecondOfDay() - start.toSecondOfDay()) / 60) < STEP_MIN)
            throw new ResponseStatusException(BAD_REQUEST, "Aralıq ən az " + STEP_MIN + " dəqiqə olmalıdır");
        if (date.isBefore(LocalDate.now()))
            throw new ResponseStatusException(BAD_REQUEST, "Keçmiş tarixə aralıq əlavə edilə bilməz");

        AvailabilityDay d = days.findByDate(date).orElseGet(() -> {
            AvailabilityDay nd = new AvailabilityDay();
            nd.setDate(date);
            return days.save(nd);
        });
        if (d.isClosed())
            throw new ResponseStatusException(CONFLICT, "Bağlı günə aralıq əlavə edilə bilməz");

        List<AvailabilityInterval> existing = intervals.findByDayOrderByStartTimeAsc(d);
        for (AvailabilityInterval iv : existing) {
            if (start.isBefore(iv.getEndTime()) && iv.getStartTime().isBefore(end))
                throw new ResponseStatusException(CONFLICT, "Bu aralıq mövcud olanla üst-üstə düşür");
        }

        AvailabilityInterval iv = new AvailabilityInterval();
        iv.setDay(d);
        iv.setStartTime(start);
        iv.setEndTime(end);
        return intervals.save(iv);
    }

    @Transactional
    public void removeInterval(LocalDate date, LocalTime start) {
        AvailabilityDay d = days.findByDate(date).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Gün tapılmadı"));
        AvailabilityInterval iv = intervals.findByDayAndStartTime(d, start).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Aralıq tapılmadı"));
        // Refuse deletion if any active reservation falls inside this interval
        boolean covered = reservations.findByReservationDateAndStatus(date, ReservationStatus.ACTIVE).stream()
                .anyMatch(r -> !r.getReservationTime().isBefore(iv.getStartTime())
                        && !r.getReservationTime().plusMinutes(r.getDurationMin()).isAfter(iv.getEndTime()));
        if (covered)
            throw new ResponseStatusException(CONFLICT, "Bu aralıqda aktiv rezerv var — əvvəlcə ləğv edin");
        intervals.delete(iv);
    }

    /**
     * Backend booking check: is [start, start+duration) fully contained in
     * some working interval AND not overlapping any active reservation?
     */
    @Transactional(readOnly = true)
    public boolean canBook(LocalDate date, LocalTime start, int durationMin) {
        if (durationMin <= 0 || durationMin % STEP_MIN != 0) return false;
        if (start.getMinute() % STEP_MIN != 0) return false;
        AvailabilityDay d = days.findByDate(date).orElse(null);
        if (d == null || d.isClosed()) return false;
        LocalTime end = start.plusMinutes(durationMin);
        boolean fits = intervals.findByDayOrderByStartTimeAsc(d).stream()
                .anyMatch(iv -> !start.isBefore(iv.getStartTime()) && !end.isAfter(iv.getEndTime()));
        if (!fits) return false;
        List<Reservation> dayRes = reservations.findByReservationDateAndStatus(date, ReservationStatus.ACTIVE);
        return !overlapsAny(dayRes, start, durationMin);
    }

    private boolean hasActive(LocalDate date) {
        return !reservations.findByReservationDateAndStatus(date, ReservationStatus.ACTIVE).isEmpty();
    }
}
