package com.nextstep.rentacar.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Optional details recorded when a job is completed; fields left null keep their current values. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCompleteRequestDto {

    /** Actual cost (replaces the estimate) */
    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal cost;

    /** Odometer at the time of service, in the fleet's mileage unit; also updates the car's mileage */
    @PositiveOrZero
    private Integer odometerReading;

    @Size(max = 2000)
    private String notes;

    /** When the car is next due for service */
    @Future
    private LocalDate nextServiceDate;
}
