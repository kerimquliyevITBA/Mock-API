package com.example.mockapi.dto;

public record ConflictDto(
        String id,
        String start,
        String end,
        String title,
        String userName
) {}
