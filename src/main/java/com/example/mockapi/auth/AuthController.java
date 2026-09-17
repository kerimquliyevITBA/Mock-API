package com.example.mockapi.auth;

import com.example.mockapi.auth.AuthDtos.AppUserDto;
import com.example.mockapi.auth.AuthDtos.ChangePasswordRequest;
import com.example.mockapi.auth.AuthDao.Credentials;
import com.example.mockapi.auth.AuthDtos.LoginRequest;
import com.example.mockapi.auth.AuthDtos.LoginResponse;
import com.example.mockapi.auth.AuthDtos.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
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
        String username = found.get().username();
        return new LoginResponse(jwt.generate(username), "Bearer", username, jwt.getExpiryMs());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of("username", auth.getName());
    }

    @PostMapping("/register")
    public ResponseEntity<AppUserDto> register(@RequestBody RegisterRequest req) {
        String username = req.username() == null ? "" : req.username().trim();
        if (username.isEmpty() || req.password() == null || req.password().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "İstifadəçi adı və parol tələb olunur");
        }
        if (dao.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu istifadəçi adı artıq mövcuddur");
        }
        AppUserDto created = dao.insert(username, encoder.encode(req.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users")
    public List<AppUserDto> users() {
        return dao.listUsers();
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
}
