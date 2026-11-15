package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Branch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Branch entity.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

    List<Branch> findByCity(String city);

    List<Branch> findByCountry(String country);

    List<Branch> findByNameAndCity(String name, String city, Pageable pageable);

    @Query("SELECT b FROM Branch b WHERE b.city = :city AND b.country = :country")
    List<Branch> findByCityAndCountry(@Param("city") String city, @Param("country") String country);

    boolean existsByNameAndCity(String name, String city);
}
