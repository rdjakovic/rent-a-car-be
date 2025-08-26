package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired private ReservationService reservationService;
    @Autowired private BranchService branchService;
    @Autowired private CarService carService;
    @Autowired private CustomerService customerService;

    private Long branchId;
    private Long carId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        BranchRequestDto branch = new BranchRequestDto(
                "Airport",
                "1 Airport Rd",
                "Gotham",
                "USA",
                "+1999888777",
                "airport@example.com",
                "24/7",
                true
        );
        branchId = branchService.create(branch).getId();

        CarRequestDto car = new CarRequestDto(
                "WDBRF61JX1F123456",
                "Mercedes",
                "C200",
                2021,
                CarCategory.LUXURY,
                TransmissionType.AUTOMATIC,
                FuelType.GASOLINE,
                5,
                15000,
                CarStatus.AVAILABLE,
                new BigDecimal("99.50"),
                branchId,
                "Black",
                "XYZ-999",
                "POL-999"
        );
        carId = carService.create(car).getId();

        CustomerRequestDto cust = new CustomerRequestDto(
                "Jane",
                "Smith",
                "jane.smith@example.com",
                "+14443332222",
                "S7654321",
                LocalDate.now().minusYears(30),
                "2 Ave",
                "Gotham",
                "USA",
                LocalDate.now().plusYears(3)
        );
        customerId = customerService.create(cust).getId();
    }

    @Test
    @DisplayName("Create reservation, prevent overlap, compute price, and status transitions")
    void createPreventOverlapAndTransitions() {
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = LocalDate.now().plusDays(7); // 5 days
        ReservationRequestDto r1 = new ReservationRequestDto(customerId, carId, start, end, branchId, branchId, "First");
        ReservationResponseDto created = reservationService.create(r1);
        assertThat(created.getTotalPrice()).isEqualByComparingTo(new BigDecimal("497.50"));
        assertThat(created.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // Overlap prevented
        ReservationRequestDto r2 = new ReservationRequestDto(customerId, carId, start.plusDays(1), end.plusDays(1), branchId, branchId, "Second");
        assertThatThrownBy(() -> reservationService.create(r2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");

        // Confirm then complete
        reservationService.confirm(created.getId());
        ReservationResponseDto afterConfirm = reservationService.getById(created.getId());
        assertThat(afterConfirm.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        reservationService.complete(created.getId());
        ReservationResponseDto afterComplete = reservationService.getById(created.getId());
        assertThat(afterComplete.getStatus()).isEqualTo(ReservationStatus.COMPLETED);

        // List methods
        Page<ReservationResponseDto> byCar = reservationService.listByCar(carId, PageRequest.of(0, 5));
        assertThat(byCar.getTotalElements()).isGreaterThanOrEqualTo(1);

        Page<ReservationResponseDto> byFilters = reservationService.listWithFilters(customerId, carId, ReservationStatus.COMPLETED, branchId, start, end.plusDays(10), PageRequest.of(0, 5));
        assertThat(byFilters.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Update reservation recalculates total price and validates date range")
    void updateReservationRecalculatesPrice() {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now().plusDays(5); // 2 days
        ReservationRequestDto r1 = new ReservationRequestDto(customerId, carId, start, end, branchId, branchId, "Short");
        ReservationResponseDto created = reservationService.create(r1);
        assertThat(created.getTotalPrice()).isEqualByComparingTo(new BigDecimal("199.00"));

        // Extend by 3 more days (total 5)
        ReservationRequestDto update = new ReservationRequestDto(customerId, carId, start, start.plusDays(5), branchId, branchId, "Extended");
        ReservationResponseDto updated = reservationService.update(created.getId(), update);
        assertThat(updated.getTotalPrice()).isEqualByComparingTo(new BigDecimal("497.50"));

        // Invalid date range
        ReservationRequestDto invalid = new ReservationRequestDto(customerId, carId, start, start, branchId, branchId, "Invalid");
        assertThatThrownBy(() -> reservationService.update(created.getId(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
    }
}
