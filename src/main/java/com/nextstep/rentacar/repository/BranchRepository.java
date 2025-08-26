package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Branch entity.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByCity(String city);

    List<Branch> findByCountry(String country);

    Page<Branch> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT b FROM Branch b WHERE b.city = :city AND b.country = :country")
    List<Branch> findByCityAndCountry(@Param("city") String city, @Param("country") String country);

    boolean existsByNameAndCity(String name, String city);
}
