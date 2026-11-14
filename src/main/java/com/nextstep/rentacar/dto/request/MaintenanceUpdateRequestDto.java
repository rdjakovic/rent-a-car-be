package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.MaintenanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Replaces the editable fields of a job that is still SCHEDULED. The date is checked in the service
 * (it may stay in the past when only other fields change, but a new date cannot be in the past).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceUpdateRequestDto {

    @NotNull
    private MaintenanceType type;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotNull
    private LocalDate scheduledDate;

    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal cost;

    @Size(max = 2000)
    private String notes;
}
