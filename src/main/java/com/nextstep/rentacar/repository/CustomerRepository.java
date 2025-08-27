package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer entity.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByDriverLicenseNo(String driverLicenseNo);

    Page<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    Page<Customer> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    List<Customer> findByCity(String city);

    @Query("SELECT c FROM Customer c WHERE c.licenseExpiryDate <= :date")
    List<Customer> findCustomersWithExpiringLicenses(@Param("date") LocalDate date);

    @Query("""
        SELECT c FROM Customer c 
        WHERE (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:firstName IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
        AND (:lastName IS NULL OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
        AND (:city IS NULL OR LOWER(c.city) LIKE LOWER(CONCAT('%', :city, '%')))
        """)
    Page<Customer> findWithFilters(@Param("email") String email,
                                  @Param("firstName") String firstName,
                                  @Param("lastName") String lastName,
                                  @Param("city") String city,
                                  Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.city) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Customer> searchAny(@Param("search") String search, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByDriverLicenseNo(String driverLicenseNo);
}
