package com.nextstep.rentacar.domain.entity;

import com.nextstep.rentacar.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a car in the fleet with soft delete capability.
 */
@Entity
@Table(name = "cars")
@Where(clause = "deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"branch", "reservations", "maintenanceRecords"})
public class Car extends BaseEntity {

    @Column(name = "vin", nullable = false, unique = true, length = 17)
    @EqualsAndHashCode.Include
    private String vin;

    @Column(name = "make", nullable = false, length = 50)
    private String make;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "car_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private CarCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission", nullable = false, length = 20)
    private TransmissionType transmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Column(name = "seats", nullable = false)
    private Integer seats;

    @Column(name = "mileage", nullable = false)
    private Integer mileage = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CarStatus status = CarStatus.AVAILABLE;

    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Column(name = "insurance_policy", length = 100)
    private String insurancePolicy;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Maintenance> maintenanceRecords = new ArrayList<>();

    public Car(String vin, String make, String model, Integer year, CarCategory category,
               TransmissionType transmission, FuelType fuelType, Integer seats,
               BigDecimal dailyPrice, Branch branch) {
        this.vin = vin;
        this.make = make;
        this.model = model;
        this.year = year;
        this.category = category;
        this.transmission = transmission;
        this.fuelType = fuelType;
        this.seats = seats;
        this.dailyPrice = dailyPrice;
        this.branch = branch;
    }

    public String getDisplayName() {
        return year + " " + make + " " + model;
    }

    public boolean isAvailable() {
        return status == CarStatus.AVAILABLE && !deleted;
    }

    public void softDelete() {
        this.deleted = true;
        this.status = CarStatus.OUT_OF_SERVICE;
    }

    public void restore() {
        this.deleted = false;
        this.status = CarStatus.AVAILABLE;
    }
}
