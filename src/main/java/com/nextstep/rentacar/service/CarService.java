package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import com.nextstep.rentacar.dto.request.CarFilterDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CarService {

    CarResponseDto create(CarRequestDto request);

    CarResponseDto update(Long id, CarRequestDto request);

    void delete(Long id);

    void restore(Long id);

    CarResponseDto getById(Long id);

    Page<CarListResponseDto> list(Pageable pageable);

    Page<CarListResponseDto> listByBranch(Long branchId, Pageable pageable);

    Page<CarListResponseDto> listDeleted(Pageable pageable);

    Page<CarListResponseDto> findAvailable(Long branchId,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           CarCategory category,
                                           TransmissionType transmission,
                                           FuelType fuelType,
                                           Integer minSeats,
                                           BigDecimal maxPrice,
                                           Pageable pageable);

    Page<CarListResponseDto> list(CarFilterDto filter, Pageable pageable);
}
