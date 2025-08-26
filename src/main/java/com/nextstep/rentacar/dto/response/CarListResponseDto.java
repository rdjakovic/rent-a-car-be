package com.nextstep.rentacar.dto.response;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight DTO for car list operations to reduce payload size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarListResponseDto {

    private Long id;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private CarCategory category;
    private TransmissionType transmission;
    private FuelType fuelType;
    private Integer seats;
    private CarStatus status;
    private BigDecimal dailyPrice;
    private String color;
    private String displayName;
    private String branchName;
}
