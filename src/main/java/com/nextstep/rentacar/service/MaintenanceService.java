package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.dto.request.MaintenanceCompleteRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceUpdateRequestDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.dto.response.MaintenanceResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceService {

    MaintenanceResponseDto schedule(MaintenanceScheduleRequestDto request);

    MaintenanceResponseDto getById(Long maintenanceId);

    MaintenanceResponseDto update(Long maintenanceId, MaintenanceUpdateRequestDto request);

    MaintenanceResponseDto start(Long maintenanceId);

    /** @param details optional; may be null */
    MaintenanceResponseDto complete(Long maintenanceId, MaintenanceCompleteRequestDto details);

    MaintenanceResponseDto cancel(Long maintenanceId);

    Page<MaintenanceResponseDto> listByFilters(Long carId,
                                               Long employeeId,
                                               MaintenanceStatus status,
                                               MaintenanceType maintenanceType,
                                               Long branchId,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               Pageable pageable);

    List<MaintenanceResponseDto> findScheduledForDate(LocalDate date);

    /** Cars due for service within the given number of days (or overdue) that have no open job yet. */
    List<CarResponseDto> findCarsDueForService(int withinDays);
}
