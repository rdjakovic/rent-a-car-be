package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.ReservationStatus;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReservationService {

    ReservationResponseDto create(ReservationRequestDto request);

    ReservationResponseDto update(Long id, ReservationRequestDto request);

    ReservationResponseDto getById(Long id);

    void cancel(Long id);

    void confirm(Long id);

    void complete(Long id);

    Page<ReservationResponseDto> listByCustomer(Long customerId, Pageable pageable);

    Page<ReservationResponseDto> listByCar(Long carId, Pageable pageable);

    Page<ReservationResponseDto> listWithFilters(Long customerId,
                                                 Long carId,
                                                 ReservationStatus status,
                                                 Long branchId,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 Pageable pageable);

    Page<ReservationResponseDto> listWithFilters(Long customerId,
                                                 Long carId,
                                                 ReservationStatus status,
                                                 Long branchId,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 String search,
                                                 Pageable pageable);
}
