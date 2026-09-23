package com.gulnar.nail.availability;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "availability_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"day_id", "slot_time"}))
public class AvailabilitySlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id", nullable = false)
    private AvailabilityDay day;

    @Column(name = "slot_time", nullable = false) private LocalTime slotTime;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public AvailabilityDay getDay() { return day; }
    public void setDay(AvailabilityDay day) { this.day = day; }
    public LocalTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalTime slotTime) { this.slotTime = slotTime; }
}
