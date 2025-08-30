package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Reservation entity with overlap prevention.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByCustomerId(Long customerId, Pageable pageable);

    Page<Reservation> findByCarId(Long carId, Pageable pageable);

    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    List<Reservation> findByCustomerIdAndStatus(Long customerId, ReservationStatus status);

    /**
     * Check for overlapping reservations - critical for preventing double booking.
     * Uses pessimistic locking for consistency.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.car.id = :carId 
        AND r.status IN ('PENDING', 'CONFIRMED')
        AND ((r.startDate <= :endDate) AND (r.endDate >= :startDate))
        """)
    List<Reservation> findOverlappingReservations(@Param("carId") Long carId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Find reservations for a date range with filters.
     */
    @Query("""
        SELECT r FROM Reservation r 
        WHERE (:customerId IS NULL OR r.customer.id = :customerId)
        AND (:carId IS NULL OR r.car.id = :carId)
        AND (:status IS NULL OR r.status = :status)
        AND (:branchId IS NULL OR r.pickupBranch.id = :branchId)
        AND ((r.startDate <= :endDate) AND (r.endDate >= :startDate))
        ORDER BY r.startDate DESC
        """)
    Page<Reservation> findReservationsInDateRange(@Param("customerId") Long customerId,
                                                 @Param("carId") Long carId,
                                                 @Param("status") ReservationStatus status,
                                                 @Param("branchId") Long branchId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 Pageable pageable);

    /**
     * Find active reservations (PENDING or CONFIRMED).
     */
    @Query("SELECT r FROM Reservation r WHERE r.status IN ('PENDING', 'CONFIRMED')")
    List<Reservation> findActiveReservations();

    /**
     * Find reservations ending today (for completion).
     */
    @Query("SELECT r FROM Reservation r WHERE r.endDate = :date AND r.status = 'CONFIRMED'")
    List<Reservation> findReservationsEndingOnDate(@Param("date") LocalDate date);

    /**
     * Find upcoming reservations (starting tomorrow).
     */
    @Query("SELECT r FROM Reservation r WHERE r.startDate = :date AND r.status = 'CONFIRMED'")
    List<Reservation> findReservationsStartingOnDate(@Param("date") LocalDate date);

    /**
     * Get reservation statistics by status.
     */
    @Query("SELECT r.status, COUNT(r) FROM Reservation r GROUP BY r.status")
    List<Object[]> getReservationCountByStatus();

    /**
     * Find reservations with comprehensive filters.
     */
    @Query("""
        SELECT r FROM Reservation r 
        WHERE (:customerId IS NULL OR r.customer.id = :customerId)
        AND (:carId IS NULL OR r.car.id = :carId)
        AND (:status IS NULL OR r.status = :status)
        AND (:branchId IS NULL OR r.pickupBranch.id = :branchId OR r.dropoffBranch.id = :branchId)
        AND (:startDate IS NULL OR r.startDate >= :startDate)
        AND (:endDate IS NULL OR r.endDate <= :endDate)
        ORDER BY r.createdAt DESC
        """)
    Page<Reservation> findWithFilters(@Param("customerId") Long customerId,
                                     @Param("carId") Long carId,
                                     @Param("status") ReservationStatus status,
                                     @Param("branchId") Long branchId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     Pageable pageable);

    /**
     * Find reservations by search term across multiple fields with optimized JOINs.
     * Searches across customer name, email, phone, reservation ID, car details, and branch names.
     * Uses case-insensitive matching for text fields and orders by creation date.
     */
    @Query("""
        SELECT DISTINCT r FROM Reservation r 
        LEFT JOIN r.customer c 
        LEFT JOIN r.car car
        LEFT JOIN r.pickupBranch pb
        LEFT JOIN r.dropoffBranch db
        WHERE (:search IS NULL OR :search = '') OR
        (
            LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            c.phone LIKE CONCAT('%', :search, '%') OR
            CAST(r.id AS string) LIKE CONCAT('%', :search, '%') OR
            LOWER(car.make) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(car.model) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(CONCAT(car.year, ' ', car.make, ' ', car.model)) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(pb.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(db.name) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY r.createdAt DESC
        """)
    Page<Reservation> findBySearchTerm(@Param("search") String searchTerm, Pageable pageable);
}
