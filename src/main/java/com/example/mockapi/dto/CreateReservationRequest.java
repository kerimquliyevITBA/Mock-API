package com.example.mockapi.dto;

public record CreateReservationRequest(
        String roomId,
        String userId,
        String date,
        String start,
        String end,
        String title
) {}
