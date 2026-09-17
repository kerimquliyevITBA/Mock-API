package com.example.mockapi.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

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
        jdbc.execute("""
            create table if not exists app_users (
              id            uuid primary key default gen_random_uuid(),
              username      text not null unique,
              password_hash text not null,
              created_at    timestamptz not null default now()
            )
            """);

        Integer count = jdbc.queryForObject("select count(*) from app_users", Integer.class);
        if (count == null || count == 0) {
            jdbc.update("insert into app_users (username, password_hash) values (?, ?)",
                    "test", encoder.encode("123"));
        }
    }
}
