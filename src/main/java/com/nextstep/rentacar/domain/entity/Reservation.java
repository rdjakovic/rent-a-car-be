package com.nextstep.rentacar.domain.entity;

import com.nextstep.rentacar.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a car rental reservation.
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"customer", "car", "pickupBranch", "dropoffBranch", "payments"})
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "start_date", nullable = false)
    @EqualsAndHashCode.Include
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @EqualsAndHashCode.Include
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_branch_id", nullable = false)
    private Branch pickupBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_branch_id", nullable = false)
    private Branch dropoffBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    public Reservation(Customer customer, Car car, LocalDate startDate, LocalDate endDate,
                      Branch pickupBranch, Branch dropoffBranch, BigDecimal totalPrice) {
        this.customer = customer;
        this.car = car;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pickupBranch = pickupBranch;
        this.dropoffBranch = dropoffBranch;
        this.totalPrice = totalPrice;
    }

    public long getDurationInDays() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public boolean canBeCancelled() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public boolean canBeConfirmed() {
        return status == ReservationStatus.PENDING;
    }

    public boolean canBeCompleted() {
        return status == ReservationStatus.CONFIRMED && !endDate.isBefore(LocalDate.now());
    }

    public void confirm() {
        if (!canBeConfirmed()) {
            throw new IllegalStateException("Reservation cannot be confirmed in current status: " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Reservation cannot be cancelled in current status: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void complete() {
        if (!canBeCompleted()) {
            throw new IllegalStateException("Reservation cannot be completed in current status: " + status);
        }
        this.status = ReservationStatus.COMPLETED;
    }
}
