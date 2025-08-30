package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CarFilterDto {
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private CarCategory category;
    private TransmissionType transmission;
    private FuelType fuelType;
    private Integer minSeats;
    private BigDecimal maxPrice;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableTo;
}
