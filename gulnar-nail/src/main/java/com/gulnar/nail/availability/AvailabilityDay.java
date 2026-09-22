package com.gulnar.nail.availability;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "availability_days")
public class AvailabilityDay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "day_date", nullable = false, unique = true) private LocalDate date;
    @Column(name = "is_closed", nullable = false) private boolean closed = false;
    @Column(length = 200) private String note;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AvailabilitySlot> slots = new ArrayList<>();

    @PreUpdate void touch() { this.updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<AvailabilitySlot> getSlots() { return slots; }
}
