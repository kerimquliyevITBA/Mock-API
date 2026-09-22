package com.gulnar.nail.reservation;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReservationController {

    private final ReservationService svc;

    public ReservationController(ReservationService svc) { this.svc = svc; }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationDto.CreateRes> create(@Valid @RequestBody ReservationDto.CreateReq req) {
        Reservation r = svc.create(req);
        return ResponseEntity.status(201).body(new ReservationDto.CreateRes(
                r.getCode(), r.getReservationDate(), r.getReservationTime(),
                r.getServiceName(), r.getPriceSnapshot(), r.getCustomerName(),
                r.getPhone(), r.getStatus().name()));
    }

    @PostMapping("/reservations/status")
    public ReservationDto.ReservationView status(@Valid @RequestBody ReservationDto.StatusReq req) {
        return ReservationDto.ReservationView.of(svc.lookup(req.code(), req.prefix(), req.phone()));
    }

    @PostMapping("/reservations/cancel")
    public ReservationDto.ReservationView cancel(@Valid @RequestBody ReservationDto.StatusReq req) {
        return ReservationDto.ReservationView.of(svc.cancelByCustomer(req.code(), req.prefix(), req.phone()));
    }

    @GetMapping("/admin/reservations")
    public List<ReservationDto.ReservationView> adminList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return svc.adminList(from, to).stream().map(ReservationDto.ReservationView::of).toList();
    }

    @PostMapping("/admin/reservations/{id}/cancel")
    public ReservationDto.ReservationView adminCancel(@PathVariable Long id) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String actor = a != null ? String.valueOf(a.getPrincipal()) : "admin";
        return ReservationDto.ReservationView.of(svc.cancelByAdmin(id, actor));
    }
}
