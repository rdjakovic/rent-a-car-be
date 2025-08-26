package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Employee entity.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByUserId(Long userId);

    Page<Employee> findByBranchId(Long branchId, Pageable pageable);

    List<Employee> findByBranchIdAndIsActive(Long branchId, Boolean isActive);

    Page<Employee> findByPosition(String position, Pageable pageable);

    @Query("""
        SELECT e FROM Employee e 
        WHERE (:branchId IS NULL OR e.branch.id = :branchId)
        AND (:position IS NULL OR LOWER(e.position) LIKE LOWER(CONCAT('%', :position, '%')))
        AND (:isActive IS NULL OR e.isActive = :isActive)
        ORDER BY e.user.lastName, e.user.firstName
        """)
    Page<Employee> findWithFilters(@Param("branchId") Long branchId,
                                  @Param("position") String position,
                                  @Param("isActive") Boolean isActive,
                                  Pageable pageable);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByUserId(Long userId);
}
