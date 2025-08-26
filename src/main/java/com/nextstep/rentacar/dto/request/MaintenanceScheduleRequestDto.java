package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.MaintenanceType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class MaintenanceScheduleRequestDto {

    @NotNull
    private Long carId;

    @NotNull
    private MaintenanceType type;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotNull
    @FutureOrPresent
    private LocalDate scheduledDate;

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public MaintenanceType getType() {
        return type;
    }

    public void setType(MaintenanceType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }
}
