package com.gulnar.nail.availability;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AvailabilityDto {

    public static final int STEP_MIN = 20;

    public record IntervalView(
            Long id,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime start,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime end
    ) {}

    public record SlotView(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime time,
            boolean taken,
            boolean past
    ) {}

    public record DayView(
            LocalDate date,
            boolean closed,
            String note,
            List<IntervalView> intervals,
            List<SlotView> slots
    ) {}

    public record UpsertDayReq(
            @NotNull LocalDate date,
            Boolean closed,
            String note
    ) {}

    public record AddIntervalReq(
            @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime start,
            @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime end
    ) {}
}
