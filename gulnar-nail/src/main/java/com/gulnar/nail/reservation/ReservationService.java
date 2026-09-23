package com.gulnar.nail.reservation;

import com.gulnar.nail.availability.AvailabilityService;
import com.gulnar.nail.service.ServiceEntity;
import com.gulnar.nail.service.ServiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.http.HttpStatus.*;

@Service
public class ReservationService {

    private final ReservationRepository repo;
    private final ServiceRepository services;
    private final AvailabilityService availability;

    public ReservationService(ReservationRepository repo, ServiceRepository services, AvailabilityService availability) {
        this.repo = repo;
        this.services = services;
        this.availability = availability;
    }

    private static String buildPhone(String prefix, String local) {
        String p = prefix.startsWith("0") ? prefix.substring(1) : prefix;
        return "+994 " + p + " " + local;
    }

    @Transactional
    public Reservation create(ReservationDto.CreateReq req) {
        String phone = buildPhone(req.prefix(), req.phone());

        ServiceEntity svc = services.findById(req.serviceId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Xidmət tapılmadı"));
        if (!svc.isActive()) throw new ResponseStatusException(BAD_REQUEST, "Bu xidmət aktiv deyil");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (req.date().isBefore(today) || (req.date().equals(today) && req.time().isBefore(now)))
            throw new ResponseStatusException(BAD_REQUEST, "Keçmiş tarix/saata rezerv olmaz");

        int duration = svc.getDurationMin();

        if (!availability.isSlotOpen(req.date(), req.time()))
            throw new ResponseStatusException(CONFLICT, "Bu tarix/saat açıq deyil");

        if (repo.existsByReservationDateAndReservationTimeAndStatus(req.date(), req.time(), ReservationStatus.ACTIVE))
            throw new ResponseStatusException(CONFLICT, "Bu saat artıq tutulub");

        if (repo.existsByPhoneAndReservationDateAndReservationTimeAndStatus(phone, req.date(), req.time(), ReservationStatus.ACTIVE))
            throw new ResponseStatusException(CONFLICT, "Bu nömrə ilə eyni tarix/saata rezerv artıq var");

        Reservation r = new Reservation();
        r.setCode(generateCode());
        r.setCustomerName(req.name().trim());
        r.setPhone(phone);
        r.setServiceId(svc.getId());
        r.setServiceName(svc.getName());
        r.setPriceSnapshot(svc.getPrice());
        r.setReservationDate(req.date());
        r.setReservationTime(req.time());
        r.setDurationMin(duration);
        r.setStatus(ReservationStatus.ACTIVE);

        try {
            return repo.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "Bu saat az öncə tutuldu — başqa saat seçin");
        }
    }

    @Transactional(readOnly = true)
    public Reservation lookup(String code, String prefix, String local) {
        String phone = buildPhone(prefix, local);
        return repo.findByCodeAndPhone(code.trim().toUpperCase(), phone)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bu kod və nömrə ilə rezerv tapılmadı"));
    }

    @Transactional
    public Reservation cancelByCustomer(String code, String prefix, String local) {
        Reservation r = lookup(code, prefix, local);
        if (r.getStatus() != ReservationStatus.ACTIVE)
            throw new ResponseStatusException(CONFLICT, "Yalnız aktiv rezervi ləğv etmək olar");
        LocalDate today = LocalDate.now();
        if (r.getReservationDate().isBefore(today))
            throw new ResponseStatusException(CONFLICT, "Keçmiş rezervi ləğv etmək olmaz");
        r.setStatus(ReservationStatus.CANCELLED);
        r.setCancelledAt(OffsetDateTime.now());
        r.setCancelledBy("CUSTOMER");
        return repo.save(r);
    }

    @Transactional
    public Reservation cancelByAdmin(Long id, String actor) {
        Reservation r = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Rezerv tapılmadı"));
        if (r.getStatus() != ReservationStatus.ACTIVE)
            throw new ResponseStatusException(CONFLICT, "Yalnız aktiv rezervi ləğv etmək olar");
        r.setStatus(ReservationStatus.CANCELLED);
        r.setCancelledAt(OffsetDateTime.now());
        r.setCancelledBy("ADMIN:" + actor);
        return repo.save(r);
    }

    @Transactional(readOnly = true)
    public List<Reservation> adminList(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now();
        if (to == null) to = from.plusDays(30);
        return repo.findByReservationDateBetweenOrderByReservationDateAscReservationTimeAsc(from, to);
    }

    private String generateCode() {
        for (int i = 0; i < 8; i++) {
            String c = "GN-" + (1000 + ThreadLocalRandom.current().nextInt(9000));
            if (!repo.existsByCode(c)) return c;
        }
        throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Kod generasiya olunmadı");
    }
}
