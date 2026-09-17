package com.example.mockapi.dto;

public record ReservationDto(
        String id,
        String roomId,
        String roomName,
        String userId,
        String userName,
        String date,
        String start,
        String end,
        String title,
        String status,
        String createdAt
) {}
