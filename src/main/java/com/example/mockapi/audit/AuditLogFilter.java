package com.example.mockapi.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@Order(1)
public class AuditLogFilter extends OncePerRequestFilter {

    private final AuditLogDao dao;

    public AuditLogFilter(AuditLogDao dao) {
        this.dao = dao;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        if ("/".equals(path)) return true;
        if (!path.startsWith("/api/")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())
                    ? auth.getName() : null;
            String role = null;
            if (auth != null) {
                role = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5))
                        .findFirst().orElse(null);
            }

            String method = request.getMethod();
            String path = request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) path = path + "?" + query;
            int status = wrappedResponse.getStatus();

            String detail = buildDetail(method, path, status);
            String ip = request.getRemoteAddr();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                ip = forwarded.split(",")[0].trim();
            }

            dao.insert(username, role, method, path, status, detail, ip);
        } catch (Exception ignored) {
        }

        wrappedResponse.copyBodyToResponse();
    }

    private String buildDetail(String method, String path, int status) {
        String cleanPath = path.contains("?") ? path.substring(0, path.indexOf('?')) : path;

        if (cleanPath.equals("/api/auth/login")) {
            return status == 200 ? "Ugurlu giris" : "Ugursuz giris cehdi";
        }
        if (cleanPath.equals("/api/auth/register")) {
            return status == 201 ? "Yeni istifadeci yaradildi" : "Istifadeci yaratma ugursuz";
        }
        if (cleanPath.startsWith("/api/auth/users") && "DELETE".equals(method)) {
            return status < 300 ? "Istifadeci silindi" : "Istifadeci silme ugursuz";
        }
        if (cleanPath.equals("/api/auth/change-password")) {
            return status == 200 ? "Parol deyisdirildi" : "Parol deyisme ugursuz";
        }
        if (cleanPath.startsWith("/api/auth/roles") && "PUT".equals(method)) {
            return "Rol icazeleri yenilendi";
        }
        if (cleanPath.equals("/api/reservations") && "POST".equals(method)) {
            return status == 201 ? "Yeni rezervasiya yaradildi" : "Rezervasiya yaratma ugursuz";
        }
        if (cleanPath.startsWith("/api/reservations") && "PUT".equals(method)) {
            return status == 200 ? "Rezervasiya yenilendi" : "Rezervasiya yenileme ugursuz";
        }
        if (cleanPath.startsWith("/api/reservations") && "DELETE".equals(method)) {
            return status == 200 ? "Rezervasiya legv edildi" : "Rezervasiya legvetme ugursuz";
        }
        if ("GET".equals(method)) {
            return "Melumat sorgusu";
        }
        return method + " " + cleanPath;
    }
}
