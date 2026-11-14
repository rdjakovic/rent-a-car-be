package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Inactive (closed) branches are not offered for booking. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BranchActiveStatusIntegrationTest {

    @Autowired private BranchService branchService;
    @Autowired private CarService carService;
    @Autowired private CustomerService customerService;
    @Autowired private ReservationService reservationService;

    private Long openBranchId;
    private Long closedBranchId;
    private Long carAtOpenBranch;
    private Long carAtClosedBranch;
    private Long customerId;

    @BeforeEach
    void setUp() {
        openBranchId = branchService.create(branch("Open Branch", "open@example.com", true)).getId();
        closedBranchId = branchService.create(branch("Closed Branch", "closed@example.com", false)).getId();
        carAtOpenBranch = createCar("WDBRF61JX1F300001", "ACT-001", openBranchId);
        carAtClosedBranch = createCar("WDBRF61JX1F300002", "ACT-002", closedBranchId);
        customerId = customerService.create(new CustomerRequestDto("Cleo", "Closed", "cleo.closed@example.com",
                "+14443338888", "C1234567", LocalDate.now().minusYears(30), "2 Ave", "Gotham", "USA",
                LocalDate.now().plusYears(3))).getId();
    }

    @Test
    @DisplayName("The active status is stored, returned, and defaults to active")
    void statusIsStoredAndReturned() {
        assertThat(branchService.getById(openBranchId).isActive()).isTrue();
        assertThat(branchService.getById(closedBranchId).isActive()).isFalse();

        BranchRequestDto noStatus = branch("No Status", "nostatus@example.com", null);
        assertThat(branchService.create(noStatus).isActive()).isTrue();
    }

    @Test
    @DisplayName("An update without the status leaves it unchanged; with it, switches it")
    void updateKeepsOrChangesStatus() {
        branchService.update(closedBranchId, branch("Closed Branch (renamed)", "closed@example.com", null));
        assertThat(branchService.getById(closedBranchId).isActive()).isFalse();

        branchService.update(closedBranchId, branch("Closed Branch (renamed)", "closed@example.com", true));
        assertThat(branchService.getById(closedBranchId).isActive()).isTrue();
    }

    @Test
    @DisplayName("Cars of an inactive branch are not offered in availability search")
    void availabilityHidesInactiveBranchCars() {
        LocalDate day = LocalDate.now().plusDays(5);

        assertThat(availableAt(openBranchId, day)).contains(carAtOpenBranch);
        assertThat(availableAt(closedBranchId, day)).doesNotContain(carAtClosedBranch).isEmpty();

        branchService.update(closedBranchId, branch("Closed Branch", "closed@example.com", true));
        assertThat(availableAt(closedBranchId, day)).contains(carAtClosedBranch);
    }

    @Test
    @DisplayName("An inactive branch can't be the pick-up or return location of a new reservation")
    void createRejectsInactiveBranch() {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);

        assertThatThrownBy(() -> reservationService.create(
                new ReservationRequestDto(customerId, carAtOpenBranch, start, end, closedBranchId, openBranchId, "x")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Pickup branch is inactive");
        assertThatThrownBy(() -> reservationService.create(
                new ReservationRequestDto(customerId, carAtOpenBranch, start, end, openBranchId, closedBranchId, "x")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Dropoff branch is inactive");

        assertThat(reservationService.create(
                new ReservationRequestDto(customerId, carAtOpenBranch, start, end, openBranchId, openBranchId, "x")).getId())
                .isNotNull();
    }

    @Test
    @DisplayName("Editing can't move a reservation to an inactive branch, but one whose branch closed later can still be edited")
    void updateRules() {
        LocalDate start = LocalDate.now().plusDays(3);
        Long id = reservationService.create(
                new ReservationRequestDto(customerId, carAtOpenBranch, start, start.plusDays(2), openBranchId, openBranchId, "x")).getId();

        assertThatThrownBy(() -> reservationService.update(id,
                new ReservationRequestDto(customerId, carAtOpenBranch, start, start.plusDays(2), openBranchId, closedBranchId, "x")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Dropoff branch is inactive");

        // The branch closes after the booking was made: dates can still be changed while keeping the branch
        branchService.update(openBranchId, branch("Open Branch", "open@example.com", false));
        var updated = reservationService.update(id,
                new ReservationRequestDto(customerId, carAtOpenBranch, start, start.plusDays(4), openBranchId, openBranchId, "x"));
        assertThat(updated.getEndDate()).isEqualTo(start.plusDays(4));
    }

    private List<Long> availableAt(Long branchId, LocalDate day) {
        return carService.findAvailable(branchId, day, day.plusDays(1), null, null, null, null, null, PageRequest.of(0, 50))
                .getContent().stream().map(CarListResponseDto::getId).toList();
    }

    private Long createCar(String vin, String plate, Long branchId) {
        return carService.create(new CarRequestDto(vin, "Acme", "Zephyr", 2022, CarCategory.COMPACT,
                TransmissionType.AUTOMATIC, FuelType.GASOLINE, 5, 1000, CarStatus.AVAILABLE,
                new BigDecimal("40.00"), branchId, "Blue", plate, "POL-ACT")).getId();
    }

    private static BranchRequestDto branch(String name, String email, Boolean active) {
        return new BranchRequestDto(name, "1 Main St", "Gotham", "USA", "+1999888777", email, "24/7", active);
    }
}
