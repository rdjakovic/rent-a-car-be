package com.nextstep.rentacar.testutil.builders;

import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Test data builder for Car-related DTOs and entities.
 * Uses fluent builder pattern to reduce test boilerplate.
 */
public class CarTestDataBuilder {
    
    private String vin = "12345678901234567";
    private String make = "Toyota";
    private String model = "Corolla";
    private Integer year = 2022;
    private CarCategory category = CarCategory.COMPACT;
    private TransmissionType transmission = TransmissionType.AUTOMATIC;
    private FuelType fuelType = FuelType.GASOLINE;
    private Integer seats = 5;
    private Integer mileage = 10000;
    private BigDecimal dailyPrice = new BigDecimal("49.99");
    private Long branchId;
    private String color = "Silver";
    private String licensePlate = "TEST-" + System.currentTimeMillis() % 10000;
    private String insurancePolicy = "POL-" + System.currentTimeMillis() % 100000;
    
    private CarTestDataBuilder() {
    }
    
    public static CarTestDataBuilder aCar() {
        return new CarTestDataBuilder();
    }
    
    public CarTestDataBuilder withVin(String vin) {
        this.vin = vin;
        return this;
    }
    
    public CarTestDataBuilder withMake(String make) {
        this.make = make;
        return this;
    }
    
    public CarTestDataBuilder withModel(String model) {
        this.model = model;
        return this;
    }
    
    public CarTestDataBuilder withYear(Integer year) {
        this.year = year;
        return this;
    }
    
    public CarTestDataBuilder withCategory(CarCategory category) {
        this.category = category;
        return this;
    }
    
    public CarTestDataBuilder asLuxury() {
        this.category = CarCategory.LUXURY;
        this.dailyPrice = new BigDecimal("199.99");
        return this;
    }
    
    public CarTestDataBuilder asSUV() {
        this.category = CarCategory.SUV;
        this.make = "Ford";
        this.model = "Explorer";
        this.seats = 7;
        this.dailyPrice = new BigDecimal("89.99");
        return this;
    }
    
    public CarTestDataBuilder asEconomy() {
        this.category = CarCategory.ECONOMY;
        this.dailyPrice = new BigDecimal("29.99");
        return this;
    }
    
    public CarTestDataBuilder asElectric() {
        this.fuelType = FuelType.ELECTRIC;
        this.make = "Tesla";
        this.model = "Model 3";
        return this;
    }
    
    public CarTestDataBuilder asHybrid() {
        this.fuelType = FuelType.HYBRID;
        this.make = "Toyota";
        this.model = "Prius";
        return this;
    }
    
    public CarTestDataBuilder withTransmission(TransmissionType transmission) {
        this.transmission = transmission;
        return this;
    }
    
    public CarTestDataBuilder withManualTransmission() {
        this.transmission = TransmissionType.MANUAL;
        return this;
    }
    
    public CarTestDataBuilder withFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
        return this;
    }
    
    public CarTestDataBuilder withSeats(Integer seats) {
        this.seats = seats;
        return this;
    }
    
    public CarTestDataBuilder withMileage(Integer mileage) {
        this.mileage = mileage;
        return this;
    }
    
    public CarTestDataBuilder withDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
        return this;
    }
    
    public CarTestDataBuilder withDailyPrice(String dailyPrice) {
        this.dailyPrice = new BigDecimal(dailyPrice);
        return this;
    }
    
    public CarTestDataBuilder withBranchId(Long branchId) {
        this.branchId = branchId;
        return this;
    }
    
    public CarTestDataBuilder withColor(String color) {
        this.color = color;
        return this;
    }
    
    public CarTestDataBuilder withLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
        return this;
    }
    
    public CarTestDataBuilder withInsurancePolicy(String insurancePolicy) {
        this.insurancePolicy = insurancePolicy;
        return this;
    }
    
    public CarRequestDto build() {
        CarRequestDto dto = new CarRequestDto();
        dto.setVin(vin);
        dto.setMake(make);
        dto.setModel(model);
        dto.setYear(year);
        dto.setCategory(category);
        dto.setTransmission(transmission);
        dto.setFuelType(fuelType);
        dto.setSeats(seats);
        dto.setMileage(mileage);
        dto.setDailyPrice(dailyPrice);
        dto.setBranchId(branchId);
        dto.setColor(color);
        dto.setLicensePlate(licensePlate);
        dto.setInsurancePolicy(insurancePolicy);
        return dto;
    }
}
