package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MaintenanceServiceIntegrationTest {

    @Autowired private MaintenanceService maintenanceService;
    @Autowired private CarService carService;
    @Autowired private BranchService branchService;

    private Long carId;

    @BeforeEach
    void setup() {
        Long branchId = branchService.create(new BranchRequestDto(
                "Service Hub",
                "500 Industrial Rd",
                "Central City",
                "USA",
                "+18887776666",
                "hub@example.com",
                "Mon-Fri 9-18",
                true
        )).getId();

        carId = carService.create(new CarRequestDto(
                "JH4KA9650MC000001",
                "Honda",
                "Accord",
                2019,
                CarCategory.STANDARD,
                TransmissionType.MANUAL,
                FuelType.GASOLINE,
                5,
                20000,
                CarStatus.AVAILABLE,
                new BigDecimal("49.00"),
                branchId,
                "White",
                "SERV-001",
                "POL-1001"
        )).getId();
    }

    @Test
    @DisplayName("Schedule -> Start sets car to MAINTENANCE; Complete/Cancel restores to AVAILABLE")
    void maintenanceLifecycleUpdatesCarStatus() {
        var m = maintenanceService.schedule(carId, MaintenanceType.ROUTINE, "Oil change", LocalDate.now().plusDays(1));
        assertThat(m.getId()).isNotNull();
        assertThat(m.getStatus()).isEqualTo(MaintenanceStatus.SCHEDULED);

        var inProgress = maintenanceService.start(m.getId());
        assertThat(inProgress.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);

        // Complete -> AVAILABLE
        var completed = maintenanceService.complete(m.getId());
        assertThat(completed.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);

        // Schedule again and cancel
        var m2 = maintenanceService.schedule(carId, MaintenanceType.INSPECTION, "Inspection", LocalDate.now().plusDays(2));
        maintenanceService.start(m2.getId());
        var cancelled = maintenanceService.cancel(m2.getId());
        assertThat(cancelled.getStatus()).isEqualTo(MaintenanceStatus.CANCELLED);
    }
}
