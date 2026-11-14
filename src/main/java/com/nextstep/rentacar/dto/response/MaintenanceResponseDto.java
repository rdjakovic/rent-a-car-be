package com.nextstep.rentacar.dto.response;

import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponseDto {

    private Long id;
    private CarListResponseDto car;
    private Long employeeId;
    private String employeeName;
    private MaintenanceType maintenanceType;
    private String description;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private BigDecimal cost;
    private String currency;
    private Integer odometerReading;
    private MaintenanceStatus status;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
