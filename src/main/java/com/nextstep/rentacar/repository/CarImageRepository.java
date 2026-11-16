package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.CarImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarImageRepository extends JpaRepository<CarImage, Long> {

    Optional<CarImage> findByIdAndCarId(Long id, Long carId);

    List<CarImage> findByCarIdOrderByPositionAsc(Long carId);

    long countByCarId(Long carId);

    /** -1 when the car has no images. */
    @Query("SELECT COALESCE(MAX(i.position), -1) FROM CarImage i WHERE i.car.id = :carId")
    int findMaxPosition(@Param("carId") Long carId);
}
