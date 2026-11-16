package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Car entity with soft delete support.
 */
@Repository
public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    Optional<Car> findByVin(String vin);

    Page<Car> findByBranchId(Long branchId, Pageable pageable);

    Page<Car> findByCategory(CarCategory category, Pageable pageable);

    Page<Car> findByStatus(CarStatus status, Pageable pageable);

    List<Car> findByBranchIdAndStatus(Long branchId, CarStatus status);

    /**
     * Find available cars for the given date range and branch.
     * Cars with maintenance scheduled within the range are excluded; cars in the shop are not AVAILABLE;
     * cars of an inactive (closed) branch are not offered.
     */
    @Query("""
        SELECT c FROM Car c 
        WHERE c.branch.id = :branchId 
        AND c.branch.active = true
        AND c.status = 'AVAILABLE' 
        AND c.deleted = false
        AND c.id NOT IN (
            SELECT r.car.id FROM Reservation r 
            WHERE r.status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
            AND ((r.startDate <= :endDate) AND (r.endDate >= :startDate))
        )
        AND c.id NOT IN (
            SELECT m.car.id FROM Maintenance m
            WHERE m.status = 'SCHEDULED'
            AND m.scheduledDate BETWEEN :startDate AND :endDate
        )
        """)
    List<Car> findAvailableCars(@Param("branchId") Long branchId, 
                               @Param("startDate") LocalDate startDate, 
                               @Param("endDate") LocalDate endDate);

    /**
     * Find available cars with filters (same availability rules as {@link #findAvailableCars}).
     */
    @Query("""
        SELECT c FROM Car c 
        WHERE c.branch.id = :branchId 
        AND c.branch.active = true
        AND c.status = 'AVAILABLE' 
        AND c.deleted = false
        AND (:category IS NULL OR c.category = :category)
        AND (:transmission IS NULL OR c.transmission = :transmission)
        AND (:fuelType IS NULL OR c.fuelType = :fuelType)
        AND (:minSeats IS NULL OR c.seats >= :minSeats)
        AND (:maxPrice IS NULL OR c.dailyPrice <= :maxPrice)
        AND c.id NOT IN (
            SELECT r.car.id FROM Reservation r 
            WHERE r.status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
            AND ((r.startDate <= :endDate) AND (r.endDate >= :startDate))
        )
        AND c.id NOT IN (
            SELECT m.car.id FROM Maintenance m
            WHERE m.status = 'SCHEDULED'
            AND m.scheduledDate BETWEEN :startDate AND :endDate
        )
        """)
    Page<Car> findAvailableCarsWithFilters(@Param("branchId") Long branchId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("category") CarCategory category,
                                          @Param("transmission") TransmissionType transmission,
                                          @Param("fuelType") FuelType fuelType,
                                          @Param("minSeats") Integer minSeats,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          Pageable pageable);

    /**
     * Find cars that need service.
     */
    @Query("SELECT c FROM Car c WHERE c.nextServiceDate <= :date AND c.status != 'OUT_OF_SERVICE'")
    List<Car> findCarsNeedingService(@Param("date") LocalDate date);

    /**
     * Cars whose next service is due on or before the given date and that have no open maintenance job.
     */
    @Query("""
        SELECT c FROM Car c
        WHERE c.nextServiceDate IS NOT NULL
        AND c.nextServiceDate <= :date
        AND c.status <> 'OUT_OF_SERVICE'
        AND NOT EXISTS (
            SELECT m FROM Maintenance m
            WHERE m.car = c
            AND m.status IN ('SCHEDULED', 'IN_PROGRESS')
        )
        ORDER BY c.nextServiceDate ASC
        """)
    List<Car> findCarsDueForServiceWithoutOpenMaintenance(@Param("date") LocalDate date);

    boolean existsByVin(String vin);

    /** Serializes photo uploads for one car so two requests can't both take the last slot. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);

    // Override to include soft-deleted cars when needed
    @Query(value = "SELECT * FROM cars WHERE id = :id", nativeQuery = true)
    Optional<Car> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM cars WHERE deleted = true", nativeQuery = true)
    Page<Car> findDeletedCars(Pageable pageable);
}
