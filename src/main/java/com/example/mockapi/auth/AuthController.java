package com.example.mockapi.auth;

import com.example.mockapi.auth.AuthDao.Credentials;
import com.example.mockapi.auth.AuthDtos.AppUserDto;
import com.example.mockapi.auth.AuthDtos.ChangePasswordRequest;
import com.example.mockapi.auth.AuthDtos.LoginRequest;
import com.example.mockapi.auth.AuthDtos.LoginResponse;
import com.example.mockapi.auth.AuthDtos.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthDao dao;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(AuthDao dao, PasswordEncoder encoder, JwtService jwt) {
        this.dao = dao;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        Optional<Credentials> found = req.username() == null ? Optional.empty() : dao.findByUsername(req.username().trim());
        if (found.isEmpty() || req.password() == null || !encoder.matches(req.password(), found.get().passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "İstifadəçi adı və ya parol yanlışdır");
        }
        Credentials c = found.get();
        String role = c.role() == null ? "USER" : c.role();
        List<String> perms = dao.permissionsForRole(role);
        return new LoginResponse(jwt.generate(c.username(), role, perms), "Bearer",
                c.username(), role, perms, jwt.getExpiryMs());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        // Freş DB oxu: admin rolun icazələrini dəyişəndə istifadəçi yenidən login etmədən də effekt görsün.
        String role = dao.findByUsername(auth.getName())
                .map(c -> c.role() == null ? "USER" : c.role())
                .orElse(roleOf(auth));
        List<String> perms = dao.permissionsForRole(role);
        return Map.of("username", auth.getName(), "role", role, "permissions", perms);
    }

    @GetMapping("/permissions")
    public List<String> allPermissions() {
        return dao.listAllPermissions();
    }

    @GetMapping("/roles")
    public List<AuthDao.RoleWithPerms> allRoles() {
        return dao.listRolesWithPermissions();
    }

    @org.springframework.web.bind.annotation.PutMapping("/roles/{name}/permissions")
    public AuthDao.RoleWithPerms setRolePerms(@PathVariable String name,
                                              @RequestBody Map<String, List<String>> body) {
        List<String> codes = body.getOrDefault("permissions", List.of());
        // yalnız mövcud kodları qəbul et
        var known = new java.util.HashSet<>(dao.listAllPermissions());
        List<String> valid = codes.stream().filter(known::contains).toList();
        dao.setRolePermissions(name.toUpperCase(), valid);
        return new AuthDao.RoleWithPerms(name.toUpperCase(), dao.permissionsForRole(name.toUpperCase()));
    }

    @PostMapping("/register")
    public ResponseEntity<AppUserDto> register(@RequestBody RegisterRequest req) {
        String username = req.username() == null ? "" : req.username().trim();
        if (username.isEmpty() || req.password() == null || req.password().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "İstifadəçi adı və parol tələb olunur");
        }
        String role = (req.role() == null || req.role().isBlank()) ? "USER" : req.role().trim().toUpperCase();
        if (!dao.roleExists(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Belə rol yoxdur: " + role);
        }
        if (dao.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu istifadəçi adı artıq mövcuddur");
        }
        AppUserDto created = dao.insert(username, encoder.encode(req.password()), role);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users")
    public List<AppUserDto> users() {
        return dao.listUsers();
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username, Authentication auth) {
        if (username.equalsIgnoreCase(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Öz hesabınızı silə bilməzsiniz");
        }
        int removed = dao.deleteByUsername(username);
        return removed == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody ChangePasswordRequest req, Authentication auth) {
        Credentials me = dao.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "İstifadəçi tapılmadı"));
        if (req.oldPassword() == null || !encoder.matches(req.oldPassword(), me.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Köhnə parol yanlışdır");
        }
        if (req.newPassword() == null || req.newPassword().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yeni parol ən azı 3 simvol olmalıdır");
        }
        dao.updatePassword(me.username(), encoder.encode(req.newPassword()));
        return Map.of("status", "ok");
    }

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst().orElse("USER");
    }
}
