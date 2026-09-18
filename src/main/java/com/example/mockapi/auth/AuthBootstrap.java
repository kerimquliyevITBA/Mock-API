package com.example.mockapi.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthBootstrap implements CommandLineRunner {

    static final String[] ALL_PERMISSIONS = {
            "USER_MANAGE", "ROLE_MANAGE", "RESERVATION_READ", "RESERVATION_WRITE", "AUDIT_READ"
    };
    static final String[] USER_DEFAULT_PERMISSIONS = {
            "RESERVATION_READ", "RESERVATION_WRITE"
    };

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public AuthBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // roles
        jdbc.execute("""
            create table if not exists roles (
              id   serial primary key,
              name text not null unique
            )
            """);
        jdbc.update("insert into roles (name) values ('ADMIN') on conflict (name) do nothing");
        jdbc.update("insert into roles (name) values ('USER')  on conflict (name) do nothing");

        // permissions
        jdbc.execute("""
            create table if not exists permissions (
              id   serial primary key,
              code text not null unique
            )
            """);
        for (String code : ALL_PERMISSIONS) {
            jdbc.update("insert into permissions (code) values (?) on conflict (code) do nothing", code);
        }

        // role_permissions
        jdbc.execute("""
            create table if not exists role_permissions (
              role_id       int not null references roles(id) on delete cascade,
              permission_id int not null references permissions(id) on delete cascade,
              primary key (role_id, permission_id)
            )
            """);

        // seed default role permissions (only if that role has no permissions yet)
        Integer adminHas = jdbc.queryForObject(
                "select count(*) from role_permissions rp join roles r on r.id=rp.role_id where r.name='ADMIN'",
                Integer.class);
        if (adminHas == null || adminHas == 0) {
            for (String code : ALL_PERMISSIONS) {
                jdbc.update(
                        "insert into role_permissions(role_id, permission_id) "
                                + "select (select id from roles where name='ADMIN'), (select id from permissions where code=?) "
                                + "on conflict do nothing",
                        code);
            }
        }
        Integer userHas = jdbc.queryForObject(
                "select count(*) from role_permissions rp join roles r on r.id=rp.role_id where r.name='USER'",
                Integer.class);
        if (userHas == null || userHas == 0) {
            for (String code : USER_DEFAULT_PERMISSIONS) {
                jdbc.update(
                        "insert into role_permissions(role_id, permission_id) "
                                + "select (select id from roles where name='USER'), (select id from permissions where code=?) "
                                + "on conflict do nothing",
                        code);
            }
        }

        // app_users
        jdbc.execute("""
            create table if not exists app_users (
              id            uuid primary key default gen_random_uuid(),
              username      text not null unique,
              password_hash text not null,
              role_id       int references roles(id),
              created_at    timestamptz not null default now()
            )
            """);
        jdbc.execute("alter table app_users add column if not exists role_id int references roles(id)");
        jdbc.update("update app_users set role_id = (select id from roles where name='USER') where role_id is null");

        Integer count = jdbc.queryForObject("select count(*) from app_users", Integer.class);
        if (count == null || count == 0) {
            jdbc.update(
                    "insert into app_users (username, password_hash, role_id) "
                            + "values (?, ?, (select id from roles where name='ADMIN'))",
                    "test", encoder.encode("123"));
        }

        // audit_logs
        jdbc.execute("""
            create table if not exists audit_logs (
              id         bigserial primary key,
              username   text,
              role       text,
              method     text not null,
              path       text not null,
              status     int not null,
              detail     text,
              ip         text,
              created_at timestamptz not null default now()
            )
            """);
    }
}
