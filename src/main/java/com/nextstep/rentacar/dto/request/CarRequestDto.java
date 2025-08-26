package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarRequestDto {

    @NotBlank(message = "VIN is required")
    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN must contain valid characters (no I, O, or Q)")
    private String vin;

    @NotBlank(message = "Make is required")
    @Size(max = 50, message = "Make should not exceed 50 characters")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model should not exceed 50 characters")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1980, message = "Year should be 1980 or later")
    @Max(value = 2030, message = "Year should not exceed 2030")
    private Integer year;

    @NotNull(message = "Category is required")
    private CarCategory category;

    @NotNull(message = "Transmission type is required")
    private TransmissionType transmission;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Number of seats is required")
    @Min(value = 2, message = "Seats should be at least 2")
    @Max(value = 20, message = "Seats should not exceed 20")
    private Integer seats;

    @Min(value = 0, message = "Mileage cannot be negative")
    private Integer mileage = 0;

    private CarStatus status = CarStatus.AVAILABLE;

    @NotNull(message = "Daily price is required")
    @DecimalMin(value = "0.01", message = "Daily price should be greater than 0")
    @DecimalMax(value = "9999.99", message = "Daily price should not exceed 9999.99")
    @Digits(integer = 4, fraction = 2, message = "Daily price should have at most 4 digits before and 2 after decimal point")
    private BigDecimal dailyPrice;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @Size(max = 30, message = "Color should not exceed 30 characters")
    private String color;

    @Size(max = 20, message = "License plate should not exceed 20 characters")
    private String licensePlate;

    @Size(max = 100, message = "Insurance policy should not exceed 100 characters")
    private String insurancePolicy;
}
