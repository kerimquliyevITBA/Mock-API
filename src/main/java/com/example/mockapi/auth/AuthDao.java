package com.example.mockapi.auth;

import com.example.mockapi.auth.AuthDtos.AppUserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthDao {

    public record Credentials(String id, String username, String passwordHash, String role) {}

    private final JdbcTemplate jdbc;

    public AuthDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Credentials> findByUsername(String username) {
        List<Credentials> found = jdbc.query(
                "select u.id, u.username, u.password_hash, r.name as role "
                        + "from app_users u left join roles r on r.id = u.role_id where u.username = ?",
                (rs, i) -> new Credentials(
                        rs.getString("id"), rs.getString("username"),
                        rs.getString("password_hash"), rs.getString("role")),
                username);
        return found.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer n = jdbc.queryForObject(
                "select count(*) from app_users where username = ?", Integer.class, username);
        return n != null && n > 0;
    }

    public AppUserDto insert(String username, String passwordHash, String roleName) {
        return jdbc.queryForObject(
                "insert into app_users (username, password_hash, role_id) "
                        + "values (?, ?, (select id from roles where name = ?)) "
                        + "returning id, username, created_at, "
                        + "(select name from roles where id = role_id) as role",
                (rs, i) -> new AppUserDto(
                        rs.getString("id"), rs.getString("username"),
                        rs.getString("role"),
                        rs.getObject("created_at", OffsetDateTime.class).toString()),
                username, passwordHash, roleName);
    }

    public int updatePassword(String username, String passwordHash) {
        return jdbc.update("update app_users set password_hash = ? where username = ?", passwordHash, username);
    }

    public int deleteByUsername(String username) {
        return jdbc.update("delete from app_users where username = ?", username);
    }

    public boolean roleExists(String roleName) {
        Integer n = jdbc.queryForObject("select count(*) from roles where name = ?", Integer.class, roleName);
        return n != null && n > 0;
    }

    public List<AppUserDto> listUsers() {
        return jdbc.query(
                "select u.id, u.username, r.name as role, u.created_at "
                        + "from app_users u left join roles r on r.id = u.role_id order by u.username",
                (rs, i) -> new AppUserDto(
                        rs.getString("id"), rs.getString("username"), rs.getString("role"),
                        rs.getObject("created_at", OffsetDateTime.class).toString()));
    }
}
