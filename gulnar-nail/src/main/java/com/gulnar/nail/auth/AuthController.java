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
    public record ChangePasswordReq(
            @NotBlank String currentPassword,
            @NotBlank @jakarta.validation.constraints.Size(min = 6, max = 100) String newPassword) {}
    public record UpdateProfileReq(
            @NotBlank @jakarta.validation.constraints.Size(min = 2, max = 120) String fullName,
            @jakarta.validation.constraints.Size(min = 3, max = 60)
            @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                    message = "Yalnız hərf, rəqəm, . _ - simvolları")
            String username) {}

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
                "fullName", u.getFullName() == null ? "" : u.getFullName(),
                "roles", roles
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        UserAccount u = currentUser();
        return ResponseEntity.ok(Map.of(
                "username", u.getUsername(),
                "fullName", u.getFullName() == null ? "" : u.getFullName(),
                "roles", u.getRoles().stream().map(Role::getName).toList()
        ));
    }

    @PostMapping("/change-password")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        UserAccount u = currentUser();
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash()))
            throw new ResponseStatusException(UNAUTHORIZED, "Cari şifrə yanlışdır");
        u.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(u);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PutMapping("/me")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        UserAccount u = currentUser();
        u.setFullName(req.fullName().trim());
        boolean usernameChanged = false;
        if (req.username() != null && !req.username().isBlank() && !req.username().equals(u.getUsername())) {
            if (users.existsByUsername(req.username()))
                throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Bu istifadəçi adı artıq mövcuddur");
            u.setUsername(req.username());
            usernameChanged = true;
        }
        users.save(u);
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("username", u.getUsername());
        resp.put("fullName", u.getFullName());
        resp.put("usernameChanged", usernameChanged);
        if (usernameChanged) {
            List<String> roles = u.getRoles().stream().map(Role::getName).toList();
            resp.put("token", jwt.generate(u.getUsername(), roles));
        }
        return ResponseEntity.ok(resp);
    }

    private UserAccount currentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal()))
            throw new ResponseStatusException(UNAUTHORIZED, "Autentifikasiya tələb olunur");
        String username = String.valueOf(auth.getPrincipal());
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "İstifadəçi tapılmadı"));
    }
}
