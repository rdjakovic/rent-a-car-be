package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.nextstep.rentacar.exception.DuplicateResourceException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CarServiceIntegrationTest {

    @Autowired
    private CarService carService;
    @Autowired
    private BranchService branchService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private EntityManager entityManager;

    private Long branchId;

    @BeforeEach
    void setUp() {
        BranchRequestDto branch = new BranchRequestDto(
                "Central Branch",
                "123 Main St",
                "Metropolis",
                "USA",
                "+1234567890",
                "branch@example.com",
                "Mon-Fri 9-17",
                true
        );
        branchId = branchService.create(branch).getId();
    }

    private CarRequestDto sampleCarRequest(String vin) {
        CarRequestDto dto = new CarRequestDto();
        dto.setVin(vin);
        dto.setMake("Toyota");
        dto.setModel("Corolla");
        dto.setYear(2020);
        dto.setCategory(CarCategory.COMPACT);
        dto.setTransmission(TransmissionType.AUTOMATIC);
        dto.setFuelType(FuelType.GASOLINE);
        dto.setSeats(5);
        dto.setMileage(10000);
        dto.setDailyPrice(new BigDecimal("39.99"));
        dto.setBranchId(branchId);
        dto.setColor("Red");
        dto.setLicensePlate("ABC-123");
        dto.setInsurancePolicy("POL123");
        return dto;
    }

    private CustomerRequestDto sampleCustomer() {
        CustomerRequestDto dto = new CustomerRequestDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john.doe@example.com");
        dto.setPhone("+15555555555");
        dto.setDriverLicenseNo("D1234567");
        dto.setDateOfBirth(LocalDate.now().minusYears(25));
        dto.setAddress("1 Test Ave");
        dto.setCity("Metropolis");
        dto.setCountry("USA");
        dto.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        return dto;
    }

    @Test
    @DisplayName("Create, update, soft delete/restore, and availability filtering for cars")
    void carLifecycleAndAvailability() {
        // Create car
        CarResponseDto created = carService.create(sampleCarRequest("1HGCM82633A004352"));
        assertThat(created.getId()).isNotNull();
        assertThat(created.getVin()).isEqualTo("1HGCM82633A004352");
        
        // Update car
        CarRequestDto update = sampleCarRequest("1HGCM82633A004352");
        update.setColor("Blue");
        CarResponseDto updated = carService.update(created.getId(), update);
        assertThat(updated.getColor()).isEqualTo("Blue");

        // Soft delete marks car OUT_OF_SERVICE
        carService.delete(created.getId());
        CarResponseDto afterDelete = carService.getById(created.getId());
        assertThat(afterDelete.getStatus()).isEqualTo(CarStatus.OUT_OF_SERVICE);

        // Restore brings it back to AVAILABLE
        carService.restore(created.getId());
        CarResponseDto afterRestore = carService.getById(created.getId());
        assertThat(afterRestore.getStatus()).isEqualTo(CarStatus.AVAILABLE);

        // Availability
        // Create a reservation that blocks availability for a given range
        Long customerId = customerService.create(sampleCustomer()).getId();
        ReservationRequestDto rr = new ReservationRequestDto(
                customerId,
                created.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10),
                branchId,
                branchId,
                "Test reservation"
        );
        ReservationResponseDto res = reservationService.create(rr);
        assertThat(res.getId()).isNotNull();

        // Query availability overlapping -> expect empty
        Page<CarListResponseDto> unavailable = carService.findAvailable(
                branchId,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(8),
                null, null, null, null, null,
                PageRequest.of(0, 10)
        );
        assertThat(unavailable.getTotalElements()).isEqualTo(0);

        // Non-overlapping -> expect 1
        Page<CarListResponseDto> available = carService.findAvailable(
                branchId,
                LocalDate.now().plusDays(11),
                LocalDate.now().plusDays(12),
                null, null, null, null, null,
                PageRequest.of(0, 10)
        );
        assertThat(available.getTotalElements()).isEqualTo(1);

        // VIN uniqueness
        assertThatThrownBy(() -> carService.create(sampleCarRequest("1HGCM82633A004352")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("VIN");
    }

    @Test
    @DisplayName("Restore car works even when entity manager is cleared (bypassing @Where)")
    void restoreWorksAfterEntityManagerClear() {
        CarResponseDto created = carService.create(sampleCarRequest("JH4KA9650MC000001"));
        // Soft delete
        carService.delete(created.getId());
        // Simulate separate transaction/session
        entityManager.flush();
        entityManager.clear();
        // Restore should find it via native query bypassing @Where
        carService.restore(created.getId());
        CarResponseDto afterRestore = carService.getById(created.getId());
        assertThat(afterRestore.getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }
}
