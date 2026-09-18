package com.example.mockapi.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/api/auth/login").permitAll()
                // öz məlumatı və parolu — hər login olan üçün
                .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                // istifadəçi idarəetməsi
                .requestMatchers(HttpMethod.POST,   "/api/auth/register").hasAuthority("PERM_USER_MANAGE")
                .requestMatchers(HttpMethod.GET,    "/api/auth/users").hasAuthority("PERM_USER_MANAGE")
                .requestMatchers(HttpMethod.DELETE, "/api/auth/users/**").hasAuthority("PERM_USER_MANAGE")
                // rol/icazə idarəetməsi
                .requestMatchers("/api/auth/roles/**", "/api/auth/permissions").hasAuthority("PERM_ROLE_MANAGE")
                // rezervasiya yazma
                .requestMatchers(HttpMethod.POST,   "/api/reservations").hasAuthority("PERM_RESERVATION_WRITE")
                .requestMatchers(HttpMethod.PUT,    "/api/reservations/**").hasAuthority("PERM_RESERVATION_WRITE")
                .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAuthority("PERM_RESERVATION_WRITE")
                // rezervasiya oxuma
                .requestMatchers(HttpMethod.GET, "/api/rooms",
                                                  "/api/reservations",
                                                  "/api/reservations/**",
                                                  "/api/availability",
                                                  "/api/conflict",
                                                  "/api/users").hasAuthority("PERM_RESERVATION_READ")
                .anyRequest().authenticated())
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":401,\"error\":\"Giriş tələb olunur\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":403,\"error\":\"Bu əməliyyat üçün icazəniz yoxdur\"}");
                }))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
