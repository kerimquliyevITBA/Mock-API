package com.example.mockapi.auth;

import com.example.mockapi.auth.AuthDtos.AppUserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthDao {

    public record Credentials(String id, String username, String passwordHash) {}

    private final JdbcTemplate jdbc;

    public AuthDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Credentials> findByUsername(String username) {
        List<Credentials> found = jdbc.query(
                "select id, username, password_hash from app_users where username = ?",
                (rs, i) -> new Credentials(rs.getString("id"), rs.getString("username"), rs.getString("password_hash")),
                username);
        return found.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer n = jdbc.queryForObject(
                "select count(*) from app_users where username = ?", Integer.class, username);
        return n != null && n > 0;
    }

    public AppUserDto insert(String username, String passwordHash) {
        return jdbc.queryForObject(
                "insert into app_users (username, password_hash) values (?, ?) returning id, username, created_at",
                (rs, i) -> new AppUserDto(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getObject("created_at", OffsetDateTime.class).toString()),
                username, passwordHash);
    }

    public int updatePassword(String username, String passwordHash) {
        return jdbc.update("update app_users set password_hash = ? where username = ?", passwordHash, username);
    }

    public List<AppUserDto> listUsers() {
        return jdbc.query(
                "select id, username, created_at from app_users order by username",
                (rs, i) -> new AppUserDto(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getObject("created_at", OffsetDateTime.class).toString()));
    }
}
