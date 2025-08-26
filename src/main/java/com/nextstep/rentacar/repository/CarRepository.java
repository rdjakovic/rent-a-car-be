package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     */
    @Query("""
        SELECT c FROM Car c 
        WHERE c.branch.id = :branchId 
        AND c.status = 'AVAILABLE' 
        AND c.deleted = false
        AND c.id NOT IN (
            SELECT r.car.id FROM Reservation r 
            WHERE r.status IN ('PENDING', 'CONFIRMED')
            AND ((r.startDate <= :endDate) AND (r.endDate >= :startDate))
        )
        """)
    List<Car> findAvailableCars(@Param("branchId") Long branchId, 
                               @Param("startDate") LocalDate startDate, 
                               @Param("endDate") LocalDate endDate);

    /**
     * Find available cars with filters.
     */
    @Query("""
        SELECT c FROM Car c 
        WHERE c.branch.id = :branchId 
        AND c.status = 'AVAILABLE' 
        AND c.deleted = false
        AND (:category IS NULL OR c.category = :category)
        AND (:transmission IS NULL OR c.transmission = :transmission)
        AND (:fuelType IS NULL OR c.fuelType = :fuelType)
        AND (:minSeats IS NULL OR c.seats >= :minSeats)
        AND (:maxPrice IS NULL OR c.dailyPrice <= :maxPrice)
        AND c.id NOT IN (
            SELECT r.car.id FROM Reservation r 
            WHERE r.status IN ('PENDING', 'CONFIRMED')
            AND ((r.startDate <= :endDate) AND (r.endDate >= :startDate))
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

    boolean existsByVin(String vin);

    // Override to include soft-deleted cars when needed
    @Query("SELECT c FROM Car c WHERE c.id = :id")
    Optional<Car> findByIdIncludingDeleted(@Param("id") Long id);

    @Query("SELECT c FROM Car c WHERE c.deleted = true")
    Page<Car> findDeletedCars(Pageable pageable);
}
