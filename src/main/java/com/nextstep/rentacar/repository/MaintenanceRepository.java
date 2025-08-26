package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Maintenance;
import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Maintenance entity.
 */
@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    Page<Maintenance> findByCarId(Long carId, Pageable pageable);

    Page<Maintenance> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<Maintenance> findByStatus(MaintenanceStatus status, Pageable pageable);

    Page<Maintenance> findByMaintenanceType(MaintenanceType maintenanceType, Pageable pageable);

    List<Maintenance> findByCarIdAndStatus(Long carId, MaintenanceStatus status);

    /**
     * Find maintenance scheduled for a specific date.
     */
    @Query("SELECT m FROM Maintenance m WHERE m.scheduledDate = :date AND m.status = 'SCHEDULED'")
    List<Maintenance> findScheduledForDate(@Param("date") LocalDate date);

    /**
     * Find overdue maintenance (scheduled date passed but not completed).
     */
    @Query("""
        SELECT m FROM Maintenance m 
        WHERE m.scheduledDate < :date 
        AND m.status IN ('SCHEDULED', 'IN_PROGRESS')
        """)
    List<Maintenance> findOverdueMaintenance(@Param("date") LocalDate date);

    /**
     * Find maintenance by date range.
     */
    @Query("""
        SELECT m FROM Maintenance m 
        WHERE m.scheduledDate BETWEEN :startDate AND :endDate
        ORDER BY m.scheduledDate ASC
        """)
    List<Maintenance> findByScheduledDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Get maintenance statistics by status.
     */
    @Query("SELECT m.status, COUNT(m) FROM Maintenance m GROUP BY m.status")
    List<Object[]> getMaintenanceCountByStatus();

    /**
     * Find maintenance with comprehensive filters.
     */
    @Query("""
        SELECT m FROM Maintenance m 
        WHERE (:carId IS NULL OR m.car.id = :carId)
        AND (:employeeId IS NULL OR m.employee.id = :employeeId)
        AND (:status IS NULL OR m.status = :status)
        AND (:maintenanceType IS NULL OR m.maintenanceType = :maintenanceType)
        AND (:branchId IS NULL OR m.car.branch.id = :branchId)
        AND (:startDate IS NULL OR m.scheduledDate >= :startDate)
        AND (:endDate IS NULL OR m.scheduledDate <= :endDate)
        ORDER BY m.scheduledDate DESC
        """)
    Page<Maintenance> findWithFilters(@Param("carId") Long carId,
                                     @Param("employeeId") Long employeeId,
                                     @Param("status") MaintenanceStatus status,
                                     @Param("maintenanceType") MaintenanceType maintenanceType,
                                     @Param("branchId") Long branchId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     Pageable pageable);

    /**
     * Find cars that need maintenance soon.
     */
    @Query("""
        SELECT m FROM Maintenance m 
        WHERE m.scheduledDate BETWEEN :today AND :futureDate
        AND m.status = 'SCHEDULED'
        ORDER BY m.scheduledDate ASC
        """)
    List<Maintenance> findUpcomingMaintenance(@Param("today") LocalDate today,
                                            @Param("futureDate") LocalDate futureDate);
}
