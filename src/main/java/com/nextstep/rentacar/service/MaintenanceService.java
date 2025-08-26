package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.entity.Maintenance;
import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceService {

    Maintenance schedule(Long carId, MaintenanceType type, String description, LocalDate scheduledDate);

    Maintenance start(Long maintenanceId);

    Maintenance complete(Long maintenanceId);

    Maintenance cancel(Long maintenanceId);

    Page<Maintenance> listByFilters(Long carId,
                                    Long employeeId,
                                    MaintenanceStatus status,
                                    MaintenanceType maintenanceType,
                                    Long branchId,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    Pageable pageable);

    List<Maintenance> findScheduledForDate(LocalDate date);
}
