package com.gulnar.nail.availability;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AvailabilityController {

    private final AvailabilityService svc;

    public AvailabilityController(AvailabilityService svc) { this.svc = svc; }

    @GetMapping("/availability")
    public List<AvailabilityDto.DayView> publicRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer durationMin) {
        return svc.range(from, to, false, durationMin);
    }

    @GetMapping("/admin/availability")
    public List<AvailabilityDto.DayView> adminRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer durationMin) {
        return svc.range(from, to, true, durationMin);
    }

    @PostMapping("/admin/availability/days")
    public ResponseEntity<?> upsertDay(@Valid @RequestBody AvailabilityDto.UpsertDayReq req) {
        var d = svc.upsertDay(req.date(), req.closed(), req.note());
        return ResponseEntity.ok(new AvailabilityDto.DayView(d.getDate(), d.isClosed(), d.getNote(), List.of(), List.of()));
    }

    @DeleteMapping("/admin/availability/days/{date}")
    public ResponseEntity<Void> deleteDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        svc.deleteDay(date);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/availability/days/{date}/intervals")
    public AvailabilityDto.IntervalView addInterval(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody AvailabilityDto.AddIntervalReq req) {
        var iv = svc.addInterval(date, req.start(), req.end());
        return new AvailabilityDto.IntervalView(iv.getId(), iv.getStartTime(), iv.getEndTime());
    }

    @DeleteMapping("/admin/availability/days/{date}/intervals/{start}")
    public ResponseEntity<Void> removeInterval(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable @DateTimeFormat(pattern = "HH:mm") LocalTime start) {
        svc.removeInterval(date, start);
        return ResponseEntity.noContent().build();
    }
}
