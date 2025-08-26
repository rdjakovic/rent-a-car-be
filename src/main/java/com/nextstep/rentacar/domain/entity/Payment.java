package com.nextstep.rentacar.domain.entity;

import com.nextstep.rentacar.domain.enums.PaymentMethod;
import com.nextstep.rentacar.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity representing a payment transaction for a reservation.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"reservation"})
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    @EqualsAndHashCode.Include
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "transaction_ref")
    @EqualsAndHashCode.Include
    private String transactionRef;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Payment(Reservation reservation, BigDecimal amount, PaymentMethod paymentMethod) {
        this.reservation = reservation;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.currency = reservation.getCurrency();
    }

    public boolean isPaid() {
        return status == PaymentStatus.CAPTURED || status == PaymentStatus.AUTHORIZED;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.CAPTURED;
    }

    public void authorize() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be authorized from PENDING status");
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.paymentDate = OffsetDateTime.now();
    }

    public void capture() {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment can only be captured from PENDING or AUTHORIZED status");
        }
        this.status = PaymentStatus.CAPTURED;
        this.paymentDate = OffsetDateTime.now();
    }

    public void fail() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be failed from PENDING status");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded in current status: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
