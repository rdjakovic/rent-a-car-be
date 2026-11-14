package com.nextstep.rentacar.domain.entity;

import com.nextstep.rentacar.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    /** Odometer when the car was handed over (fleet mileage unit) */
    @Column(name = "mileage_out")
    private Integer mileageOut;

    /** Odometer when the car came back */
    @Column(name = "mileage_in")
    private Integer mileageIn;

    @Column(name = "picked_up_at")
    private OffsetDateTime pickedUpAt;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

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
        return rentalDays(startDate, endDate);
    }

    /**
     * Number of charged rental days. A same-day rental (start == end) counts as 1 day;
     * otherwise it's the number of days between pickup and return (14th to 16th = 2 days).
     */
    public static long rentalDays(LocalDate startDate, LocalDate endDate) {
        return Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
    }

    public boolean canBeCancelled() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public boolean canBeConfirmed() {
        return status == ReservationStatus.PENDING;
    }

    /** A confirmed reservation can be picked up on any day from its start date to its end date. */
    public boolean canBePickedUp(LocalDate today) {
        return status == ReservationStatus.CONFIRMED && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /** Completing is returning the car, also when it comes back after the end date. */
    public boolean canBeCompleted() {
        return status == ReservationStatus.ACTIVE;
    }

    public void pickUp(Integer mileageOut) {
        if (!canBePickedUp(LocalDate.now())) {
            throw new IllegalStateException("Reservation can only be picked up when CONFIRMED and between its start and end date"
                    + " (status: " + status + ", dates: " + startDate + " to " + endDate + ")");
        }
        this.status = ReservationStatus.ACTIVE;
        this.pickedUpAt = OffsetDateTime.now();
        this.mileageOut = mileageOut;
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
        complete(null);
    }

    /** The car is back: records the return time and odometer (optional). */
    public void complete(Integer mileageIn) {
        if (!canBeCompleted()) {
            throw new IllegalStateException("Reservation cannot be completed in current status: " + status);
        }
        this.status = ReservationStatus.COMPLETED;
        this.returnedAt = OffsetDateTime.now();
        this.mileageIn = mileageIn;
    }
}
