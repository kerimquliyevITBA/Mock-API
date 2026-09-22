package com.gulnar.nail.service;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final ServiceRepository repo;

    public ServiceController(ServiceRepository repo) { this.repo = repo; }

    @GetMapping("/services")
    public List<ServiceDto.ServiceView> publicList() {
        return repo.findByActiveTrueOrderByNameAsc().stream().map(ServiceDto.ServiceView::of).toList();
    }

    @GetMapping("/admin/services")
    public List<ServiceDto.ServiceView> adminList() {
        return repo.findAllByOrderByNameAsc().stream().map(ServiceDto.ServiceView::of).toList();
    }

    @PostMapping("/admin/services")
    @Transactional
    public ResponseEntity<ServiceDto.ServiceView> create(@Valid @RequestBody ServiceDto.UpsertReq req) {
        ServiceEntity e = new ServiceEntity();
        apply(e, req);
        e = repo.save(e);
        return ResponseEntity.created(URI.create("/api/v1/admin/services/" + e.getId()))
                .body(ServiceDto.ServiceView.of(e));
    }

    @PutMapping("/admin/services/{id}")
    @Transactional
    public ServiceDto.ServiceView update(@PathVariable Long id, @Valid @RequestBody ServiceDto.UpsertReq req) {
        ServiceEntity e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Xidmət tapılmadı"));
        apply(e, req);
        return ServiceDto.ServiceView.of(repo.save(e));
    }

    @DeleteMapping("/admin/services/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ServiceEntity e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Xidmət tapılmadı"));
        e.setActive(false);
        repo.save(e);
        return ResponseEntity.noContent().build();
    }

    private static void apply(ServiceEntity e, ServiceDto.UpsertReq r) {
        e.setName(r.name().trim());
        e.setPrice(r.price());
        e.setDurationMin(r.durationMin() == null ? 60 : r.durationMin());
        e.setActive(r.active() == null ? true : r.active());
    }
}
