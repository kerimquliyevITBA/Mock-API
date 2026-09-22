package com.gulnar.nail.reservation;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false, unique = true, length = 20) private String code;
    @Column(name = "customer_name", nullable = false, length = 120) private String customerName;
    @Column(nullable = false, length = 30) private String phone;

    @Column(name = "service_id", nullable = false) private Long serviceId;
    @Column(name = "service_name", nullable = false, length = 120) private String serviceName;
    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2) private BigDecimal priceSnapshot;

    @Column(name = "reservation_date", nullable = false) private LocalDate reservationDate;
    @Column(name = "reservation_time", nullable = false) private LocalTime reservationTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();
    @Column(name = "cancelled_at") private OffsetDateTime cancelledAt;
    @Column(name = "cancelled_by", length = 60) private String cancelledBy;

    @PreUpdate void touch() { this.updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal priceSnapshot) { this.priceSnapshot = priceSnapshot; }
    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }
    public LocalTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalTime reservationTime) { this.reservationTime = reservationTime; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
}
