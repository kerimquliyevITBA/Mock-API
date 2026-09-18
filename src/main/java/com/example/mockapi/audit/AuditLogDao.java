package com.example.mockapi.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogDao {

    public record AuditEntry(long id, String username, String role, String method,
                             String path, int status, String detail, String ip,
                             String timestamp) {}

    private final JdbcTemplate jdbc;

    public AuditLogDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String username, String role, String method, String path,
                       int status, String detail, String ip) {
        jdbc.update(
                "insert into audit_logs (username, role, method, path, status, detail, ip) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                username, role, method, path, status, detail, ip);
    }

    public List<AuditEntry> list(String username, String method, Integer status,
                                 String from, String to, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
                "select id, username, role, method, path, status, detail, ip, created_at "
                        + "from audit_logs where 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" and username = ?");
            params.add(username);
        }
        if (method != null && !method.isBlank()) {
            sql.append(" and method = ?");
            params.add(method);
        }
        if (status != null) {
            sql.append(" and status = ?");
            params.add(status);
        }
        if (from != null && !from.isBlank()) {
            sql.append(" and created_at >= ?::timestamptz");
            params.add(from);
        }
        if (to != null && !to.isBlank()) {
            sql.append(" and created_at <= ?::timestamptz");
            params.add(to);
        }
        sql.append(" order by id desc limit ? offset ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), (rs, i) -> new AuditEntry(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("method"),
                rs.getString("path"),
                rs.getInt("status"),
                rs.getString("detail"),
                rs.getString("ip"),
                rs.getObject("created_at", OffsetDateTime.class).toString()
        ), params.toArray());
    }

    public long count(String username, String method, Integer status, String from, String to) {
        StringBuilder sql = new StringBuilder("select count(*) from audit_logs where 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" and username = ?");
            params.add(username);
        }
        if (method != null && !method.isBlank()) {
            sql.append(" and method = ?");
            params.add(method);
        }
        if (status != null) {
            sql.append(" and status = ?");
            params.add(status);
        }
        if (from != null && !from.isBlank()) {
            sql.append(" and created_at >= ?::timestamptz");
            params.add(from);
        }
        if (to != null && !to.isBlank()) {
            sql.append(" and created_at <= ?::timestamptz");
            params.add(to);
        }

        Long c = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return c == null ? 0 : c;
    }
}
