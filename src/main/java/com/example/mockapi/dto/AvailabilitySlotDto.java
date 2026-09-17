package com.example.mockapi.dto;

public record AvailabilitySlotDto(
        String start,
        String end,
        boolean free,
        String busyTitle,
        String busyBy
) {}
