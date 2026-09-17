package com.example.mockapi.dto;

import java.util.List;

public record RoomDto(
        String id,
        String name,
        int capacity,
        int floor,
        List<String> features,
        int todayActiveCount
) {}
