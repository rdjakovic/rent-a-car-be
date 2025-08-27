package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CarFilterDto {
    private CarCategory category;
    private TransmissionType transmission;
    private FuelType fuelType;
    private Integer minSeats;
    private BigDecimal maxPrice;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate availableTo;

    public CarCategory getCategory() { return category; }
    public void setCategory(CarCategory category) { this.category = category; }

    public TransmissionType getTransmission() { return transmission; }
    public void setTransmission(TransmissionType transmission) { this.transmission = transmission; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public Integer getMinSeats() { return minSeats; }
    public void setMinSeats(Integer minSeats) { this.minSeats = minSeats; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }

    public LocalDate getAvailableTo() { return availableTo; }
    public void setAvailableTo(LocalDate availableTo) { this.availableTo = availableTo; }
}

