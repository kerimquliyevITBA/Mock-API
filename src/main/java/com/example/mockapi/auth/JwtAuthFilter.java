package com.example.mockapi.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthDao authDao;

    public JwtAuthFilter(JwtService jwtService, AuthDao authDao) {
        this.jwtService = jwtService;
        this.authDao = authDao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = jwtService.parse(header.substring(7)).getPayload();
                String username = claims.getSubject();
                // Freş DB oxu: rol və icazələr hər sorğuda DB-nin cari halından götürülür.
                // Beləliklə admin icazə dəyişəndə relogin gözləmir — dərhal effekt edir.
                var credOpt = authDao.findByUsername(username);
                if (credOpt.isPresent()) {
                    String role = credOpt.get().role();
                    if (role == null || role.isBlank()) {
                        role = "USER";
                    }
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    for (String p : authDao.permissionsForRole(role)) {
                        authorities.add(new SimpleGrantedAuthority("PERM_" + p));
                    }
                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // invalid/expired token -> request stays unauthenticated
            }
        }
        chain.doFilter(request, response);
    }
}
