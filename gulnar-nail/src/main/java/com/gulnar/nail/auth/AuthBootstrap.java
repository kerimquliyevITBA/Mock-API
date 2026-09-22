package com.gulnar.nail.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthBootstrap implements CommandLineRunner {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String name;

    public AuthBootstrap(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                         @Value("${app.admin.bootstrap-username}") String username,
                         @Value("${app.admin.bootstrap-password}") String password,
                         @Value("${app.admin.bootstrap-name}") String name) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        Role admin = roles.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role(); r.setName("ADMIN"); return roles.save(r);
        });
        roles.findByName("CUSTOMER").orElseGet(() -> {
            Role r = new Role(); r.setName("CUSTOMER"); return roles.save(r);
        });
        if (!users.existsByUsername(username)) {
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setPasswordHash(encoder.encode(password));
            u.setFullName(name);
            u.setRoles(Set.of(admin));
            users.save(u);
        }
    }
}
