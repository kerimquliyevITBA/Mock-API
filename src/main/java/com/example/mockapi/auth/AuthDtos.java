package com.example.mockapi.auth;

public class AuthDtos {

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String accessToken, String tokenType, String username, String role,
                                java.util.List<String> permissions, long expiresInMs) {}

    public record RegisterRequest(String username, String password, String role) {}

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}

    public record AppUserDto(String id, String username, String role, String createdAt) {}

    private AuthDtos() {}
}
