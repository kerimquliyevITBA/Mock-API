package com.gulnar.nail.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        UserAccount u = users.findByUsername(req.username()).orElseThrow(
                () -> new ResponseStatusException(UNAUTHORIZED, "İstifadəçi adı və ya şifrə yanlışdır"));
        if (!u.isEnabled() || !encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "İstifadəçi adı və ya şifrə yanlışdır");
        }
        List<String> roles = u.getRoles().stream().map(Role::getName).toList();
        String token = jwt.generate(u.getUsername(), roles);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", u.getUsername(),
                "fullName", u.getFullName(),
                "roles", roles
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String h) {
        if (h == null || !h.startsWith("Bearer ")) throw new ResponseStatusException(UNAUTHORIZED, "no token");
        var c = jwt.parse(h.substring(7));
        return ResponseEntity.ok(Map.of("username", c.getSubject(), "roles", c.get("roles")));
    }
}
