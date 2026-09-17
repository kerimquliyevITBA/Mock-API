package com.example.mockapi;

import com.example.mockapi.dto.AvailabilitySlotDto;
import com.example.mockapi.dto.ConflictDto;
import com.example.mockapi.dto.CreateReservationRequest;
import com.example.mockapi.dto.ReservationDto;
import com.example.mockapi.dto.RoomDto;
import com.example.mockapi.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationDao dao;

    public ReservationController(ReservationDao dao) {
        this.dao = dao;
    }

    @GetMapping("/rooms")
    public List<RoomDto> rooms() {
        return dao.getRooms();
    }

    @GetMapping("/reservations")
    public List<ReservationDto> reservations(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return dao.getReservations(date, roomId, status, sort);
    }

    @GetMapping("/availability")
    public List<AvailabilitySlotDto> availability(
            @RequestParam String roomId,
            @RequestParam String date) {
        return dao.getAvailability(roomId, date);
    }

    @GetMapping("/conflict")
    public Map<String, Object> conflict(
            @RequestParam String roomId,
            @RequestParam String date,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String ignoreId) {
        ConflictDto clash = dao.checkConflict(roomId, date, start, end, ignoreId);
        Map<String, Object> body = new HashMap<>();
        body.put("conflict", clash != null);
        body.put("reservation", clash);
        return body;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto create(@RequestBody CreateReservationRequest req) {
        return dao.createReservation(req);
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<ReservationDto> cancel(@PathVariable String id) {
        ReservationDto cancelled = dao.cancelReservation(id);
        return cancelled == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(cancelled);
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return dao.getUsers();
    }
}
