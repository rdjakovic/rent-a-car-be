package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Payment;
import com.nextstep.rentacar.domain.enums.PaymentMethod;
import com.nextstep.rentacar.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByReservationId(Long reservationId, Pageable pageable);

    List<Payment> findByReservationIdAndStatus(Long reservationId, PaymentStatus status);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByPaymentMethod(PaymentMethod paymentMethod, Pageable pageable);

    Optional<Payment> findByTransactionRef(String transactionRef);

    /**
     * Find payments by date range.
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.paymentDate BETWEEN :startDate AND :endDate
        ORDER BY p.paymentDate DESC
        """)
    List<Payment> findByPaymentDateRange(@Param("startDate") OffsetDateTime startDate,
                                        @Param("endDate") OffsetDateTime endDate);

    /**
     * Get total payment amount by status.
     */
    @Query("SELECT p.status, SUM(p.amount) FROM Payment p WHERE p.currency = :currency GROUP BY p.status")
    List<Object[]> getTotalAmountByStatus(@Param("currency") String currency);

    /**
     * Find failed payments that can be retried.
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.status = 'FAILED' 
        AND p.createdAt > :cutoffDate
        ORDER BY p.createdAt DESC
        """)
    List<Payment> findRecentFailedPayments(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * Find payments with comprehensive filters.
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE (:reservationId IS NULL OR p.reservation.id = :reservationId)
        AND (:status IS NULL OR p.status = :status)
        AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod)
        AND (:customerId IS NULL OR p.reservation.customer.id = :customerId)
        AND (:minAmount IS NULL OR p.amount >= :minAmount)
        AND (:maxAmount IS NULL OR p.amount <= :maxAmount)
        AND (:startDate IS NULL OR p.paymentDate >= :startDate)
        AND (:endDate IS NULL OR p.paymentDate <= :endDate)
        ORDER BY p.paymentDate DESC
        """)
    Page<Payment> findWithFilters(@Param("reservationId") Long reservationId,
                                 @Param("status") PaymentStatus status,
                                 @Param("paymentMethod") PaymentMethod paymentMethod,
                                 @Param("customerId") Long customerId,
                                 @Param("minAmount") BigDecimal minAmount,
                                 @Param("maxAmount") BigDecimal maxAmount,
                                 @Param("startDate") OffsetDateTime startDate,
                                 @Param("endDate") OffsetDateTime endDate,
                                 Pageable pageable);

    boolean existsByTransactionRef(String transactionRef);
}
