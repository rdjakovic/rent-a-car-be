package com.nextstep.rentacar.domain.entity;

import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing maintenance records for cars.
 */
@Entity
@Table(name = "maintenance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"car", "employee"})
public class Maintenance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 50)
    @EqualsAndHashCode.Include
    private MaintenanceType maintenanceType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_date", nullable = false)
    @EqualsAndHashCode.Include
    private LocalDate scheduledDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaintenanceStatus status = MaintenanceStatus.SCHEDULED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Maintenance(Car car, MaintenanceType maintenanceType, String description, LocalDate scheduledDate) {
        this.car = car;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.scheduledDate = scheduledDate;
    }

    public boolean canBeStarted() {
        return status == MaintenanceStatus.SCHEDULED;
    }

    public boolean canBeCompleted() {
        return status == MaintenanceStatus.IN_PROGRESS;
    }

    public boolean canBeCancelled() {
        return status == MaintenanceStatus.SCHEDULED || status == MaintenanceStatus.IN_PROGRESS;
    }

    public void start() {
        if (!canBeStarted()) {
            throw new IllegalStateException("Maintenance cannot be started in current status: " + status);
        }
        this.status = MaintenanceStatus.IN_PROGRESS;
    }

    public void complete() {
        if (!canBeCompleted()) {
            throw new IllegalStateException("Maintenance cannot be completed in current status: " + status);
        }
        this.status = MaintenanceStatus.COMPLETED;
        this.completedDate = LocalDate.now();
    }

    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Maintenance cannot be cancelled in current status: " + status);
        }
        this.status = MaintenanceStatus.CANCELLED;
    }
}
