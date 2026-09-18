package com.example.mockapi.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthBootstrap implements CommandLineRunner {

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
        // köhnə cədvəldə role_id yoxdursa əlavə et (idempotent miqrasiya)
        jdbc.execute("alter table app_users add column if not exists role_id int references roles(id)");
        // rolu olmayan sətirləri USER et
        jdbc.update("update app_users set role_id = (select id from roles where name='USER') where role_id is null");

        // ilk açılışda admin seed: test/123
        Integer count = jdbc.queryForObject("select count(*) from app_users", Integer.class);
        if (count == null || count == 0) {
            jdbc.update(
                    "insert into app_users (username, password_hash, role_id) "
                            + "values (?, ?, (select id from roles where name='ADMIN'))",
                    "test", encoder.encode("123"));
        }
    }
}
