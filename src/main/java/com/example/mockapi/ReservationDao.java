package com.example.mockapi;

import com.example.mockapi.dto.AvailabilitySlotDto;
import com.example.mockapi.dto.ConflictDto;
import com.example.mockapi.dto.CreateReservationRequest;
import com.example.mockapi.dto.ReservationDto;
import com.example.mockapi.dto.RoomDto;
import com.example.mockapi.dto.UserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ReservationDao {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private static final String RESERVATION_SELECT = """
            select id, room_id, room_name, user_id, user_name,
                   res_date, start_time, end_time, title, status, created_at
            from reservation_list
            """;

    private final JdbcTemplate jdbc;

    public ReservationDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- rooms: room_overview view ----
    public List<RoomDto> getRooms() {
        return jdbc.query(
                "select id, name, capacity, floor, features, today_active_count from room_overview order by id",
                (rs, i) -> new RoomDto(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("capacity"),
                        rs.getInt("floor"),
                        readTextArray(rs, "features"),
                        rs.getInt("today_active_count")
                ));
    }

    // ---- reservations: reservation_list view + optional filters/sort ----
    public List<ReservationDto> getReservations(String date, String roomId, String status, String sort) {
        StringBuilder sql = new StringBuilder(RESERVATION_SELECT).append(" where 1=1");
        List<Object> params = new ArrayList<>();
        if (isSet(date))   { sql.append(" and res_date = ?::date");                 params.add(date); }
        if (isSet(roomId)) { sql.append(" and room_id = ?");                        params.add(roomId); }
        if (isSet(status)) { sql.append(" and status = ?::reservation_status");     params.add(status); }
        sql.append(switch (sort == null ? "date" : sort) {
            case "room"   -> " order by room_id, res_date, start_time";
            case "status" -> " order by status, res_date, start_time";
            default        -> " order by res_date, start_time";
        });
        return jdbc.query(sql.toString(), reservationMapper(), params.toArray());
    }

    public ReservationDto getReservationById(String id) {
        List<ReservationDto> found = jdbc.query(
                RESERVATION_SELECT + " where id = ?", reservationMapper(), id);
        return found.isEmpty() ? null : found.get(0);
    }

    // ---- availability: get_availability(room, date) RPC ----
    public List<AvailabilitySlotDto> getAvailability(String roomId, String date) {
        return jdbc.query(
                "select slot_start, slot_end, is_free, busy_title, busy_by from get_availability(?, ?::date)",
                (rs, i) -> new AvailabilitySlotDto(
                        time(rs, "slot_start"),
                        time(rs, "slot_end"),
                        rs.getBoolean("is_free"),
                        rs.getString("busy_title"),
                        rs.getString("busy_by")
                ),
                roomId, date);
    }

    // ---- conflict check: check_conflict(...) RPC ----
    public ConflictDto checkConflict(String roomId, String date, String start, String end, String ignoreId) {
        List<ConflictDto> found = jdbc.query(
                "select id, start_time, end_time, title, user_name "
                        + "from check_conflict(?, ?::date, ?::time, ?::time, ?::text)",
                (rs, i) -> new ConflictDto(
                        rs.getString("id"),
                        time(rs, "start_time"),
                        time(rs, "end_time"),
                        rs.getString("title"),
                        rs.getString("user_name")
                ),
                roomId, date, start, end, ignoreId);
        return found.isEmpty() ? null : found.get(0);
    }

    // ---- create: create_reservation(...) RPC — throws on 23P01/P0001 ----
    public ReservationDto createReservation(CreateReservationRequest req) {
        String newId = jdbc.queryForObject(
                "select id from create_reservation(?, ?::uuid, ?::date, ?::time, ?::time, ?)",
                String.class,
                req.roomId(), req.userId(), req.date(), req.start(), req.end(), req.title());
        return getReservationById(newId);
    }

    // ---- cancel: cancel_reservation(id) RPC ----
    // The function returns a single composite row; when nothing is updated
    // (id not found or not active) that row's id is NULL -> treat as "not cancelled".
    public ReservationDto cancelReservation(String id) {
        String cancelledId = jdbc.queryForObject(
                "select id from cancel_reservation(?)",
                String.class,
                id);
        return cancelledId == null ? null : getReservationById(cancelledId);
    }

    // ---- users: for the name dropdown ----
    public List<UserDto> getUsers() {
        return jdbc.query(
                "select id, name, email from users order by name",
                (rs, i) -> new UserDto(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email")
                ));
    }

    // ---- helpers ----
    private RowMapper<ReservationDto> reservationMapper() {
        return (rs, i) -> new ReservationDto(
                rs.getString("id"),
                rs.getString("room_id"),
                rs.getString("room_name"),
                rs.getString("user_id"),
                rs.getString("user_name"),
                rs.getObject("res_date", LocalDate.class).toString(),
                time(rs, "start_time"),
                time(rs, "end_time"),
                rs.getString("title"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class).toString()
        );
    }

    private static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }

    private static String time(ResultSet rs, String col) throws SQLException {
        LocalTime t = rs.getObject(col, LocalTime.class);
        return t == null ? null : t.format(HHMM);
    }

    private static List<String> readTextArray(ResultSet rs, String col) throws SQLException {
        Array arr = rs.getArray(col);
        if (arr == null) {
            return List.of();
        }
        return Arrays.asList((String[]) arr.getArray());
    }
}
