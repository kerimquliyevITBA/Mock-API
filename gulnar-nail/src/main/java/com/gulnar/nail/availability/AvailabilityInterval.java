package com.gulnar.nail.availability;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "availability_intervals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"day_id", "start_time"}))
public class AvailabilityInterval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id", nullable = false)
    private AvailabilityDay day;

    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time",   nullable = false) private LocalTime endTime;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public AvailabilityDay getDay() { return day; }
    public void setDay(AvailabilityDay day) { this.day = day; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int durationMinutes() {
        return (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 60;
    }
}
