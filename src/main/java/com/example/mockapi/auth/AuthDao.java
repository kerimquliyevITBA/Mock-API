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

    public List<String> permissionsForRole(String roleName) {
        return jdbc.query(
                "select p.code from permissions p "
                        + "join role_permissions rp on rp.permission_id = p.id "
                        + "join roles r on r.id = rp.role_id "
                        + "where r.name = ? order by p.code",
                (rs, i) -> rs.getString("code"),
                roleName);
    }

    public List<String> listAllPermissions() {
        return jdbc.query("select code from permissions order by code",
                (rs, i) -> rs.getString("code"));
    }

    public record RoleWithPerms(String name, List<String> permissions) {}

    public List<RoleWithPerms> listRolesWithPermissions() {
        List<String> roles = jdbc.query("select name from roles order by name",
                (rs, i) -> rs.getString("name"));
        return roles.stream().map(n -> new RoleWithPerms(n, permissionsForRole(n))).toList();
    }

    public void setRolePermissions(String roleName, List<String> permissionCodes) {
        Integer roleId = jdbc.queryForObject("select id from roles where name = ?", Integer.class, roleName);
        if (roleId == null) {
            throw new IllegalArgumentException("Belə rol yoxdur: " + roleName);
        }
        jdbc.update("delete from role_permissions where role_id = ?", roleId);
        for (String code : permissionCodes) {
            jdbc.update(
                    "insert into role_permissions(role_id, permission_id) "
                            + "values (?, (select id from permissions where code = ?)) "
                            + "on conflict do nothing",
                    roleId, code);
        }
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
