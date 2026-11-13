package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.dto.response.MaintenanceResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceService {

    MaintenanceResponseDto schedule(Long carId, MaintenanceType type, String description, LocalDate scheduledDate);

    MaintenanceResponseDto start(Long maintenanceId);

    MaintenanceResponseDto complete(Long maintenanceId);

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
}
