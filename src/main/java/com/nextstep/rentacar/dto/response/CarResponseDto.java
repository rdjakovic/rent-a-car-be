package com.nextstep.rentacar.dto.response;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponseDto {

    private Long id;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private CarCategory category;
    private TransmissionType transmission;
    private FuelType fuelType;
    private Integer seats;
    private Integer mileage;
    private CarStatus status;
    private BigDecimal dailyPrice;
    private String color;
    private String licensePlate;
    private String insurancePolicy;
    private LocalDate lastServiceDate;
    private LocalDate nextServiceDate;
    private String displayName;
    private BranchResponseDto branch;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** Current or next open booking; filled by the /cars list and details endpoints, null elsewhere */
    private NextReservationDto nextReservation;

    /** Photos in upload order; the UI shows the first as the cover. */
    private List<CarImageResponseDto> images = new ArrayList<>();
}
