package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.dto.request.ReservationPickupRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.request.ReservationReturnRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import com.nextstep.rentacar.repository.ReservationRepository;
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

import static org.assertj.core.api.Assertions.*;

/** Pick-up (CONFIRMED -> ACTIVE, car RENTED) and return (ACTIVE -> COMPLETED, car AVAILABLE). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RentalLifecycleIntegrationTest {

    @Autowired private ReservationService reservationService;
    @Autowired private CarService carService;
    @Autowired private BranchService branchService;
    @Autowired private CustomerService customerService;
    @Autowired private MaintenanceService maintenanceService;
    @Autowired private ReservationRepository reservationRepository;

    private Long branchId;
    private Long otherBranchId;
    private Long carId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        branchId = branchService.create(branch("Downtown", "down@example.com")).getId();
        otherBranchId = branchService.create(branch("Airport", "air@example.com")).getId();
        carId = carService.create(new CarRequestDto(
                "WDBRF61JX1F654321", "Mercedes", "C200", 2021, CarCategory.LUXURY, TransmissionType.AUTOMATIC,
                FuelType.GASOLINE, 5, 15000, CarStatus.AVAILABLE, new BigDecimal("99.50"), branchId,
                "Black", "RENT-001", "POL-001")).getId();
        customerId = customerService.create(new CustomerRequestDto(
                "Rita", "Renter", "rita.renter@example.com", "+14443330000", "R1234567",
                LocalDate.now().minusYears(30), "2 Ave", "Gotham", "USA", LocalDate.now().plusYears(3))).getId();
    }

    @Test
    @DisplayName("Pick-up makes the rental ACTIVE and the car RENTED; return completes it and frees the car")
    void pickUpAndReturn() {
        Long id = confirmedReservation(LocalDate.now(), LocalDate.now().plusDays(3), otherBranchId);

        reservationService.pickUp(id, new ReservationPickupRequestDto(15100));

        ReservationResponseDto active = reservationService.getById(id);
        assertThat(active.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(active.getMileageOut()).isEqualTo(15100);
        assertThat(active.getPickedUpAt()).isNotNull();
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.RENTED);
        assertThat(carService.getById(carId).getMileage()).isEqualTo(15100);

        reservationService.complete(id, new ReservationReturnRequestDto(15480));

        ReservationResponseDto completed = reservationService.getById(id);
        assertThat(completed.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(completed.getMileageIn()).isEqualTo(15480);
        assertThat(completed.getReturnedAt()).isNotNull();
        var car = carService.getById(carId);
        assertThat(car.getStatus()).isEqualTo(CarStatus.AVAILABLE);
        assertThat(car.getMileage()).isEqualTo(15480);
        // One-way rental: the car is now at the drop-off branch
        assertThat(car.getBranch().getId()).isEqualTo(otherBranchId);
    }

    @Test
    @DisplayName("Odometer and details are optional")
    void pickUpAndReturnWithoutDetails() {
        Long id = confirmedReservation(LocalDate.now(), LocalDate.now(), branchId);

        reservationService.pickUp(id, null);
        reservationService.complete(id, null);

        assertThat(reservationService.getById(id).getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(carService.getById(carId).getMileage()).isEqualTo(15000);
    }

    @Test
    @DisplayName("Pick-up only for CONFIRMED reservations within their dates")
    void pickUpRules() {
        Long pending = reservationService.create(request(LocalDate.now(), LocalDate.now().plusDays(1), branchId)).getId();
        assertThatThrownBy(() -> reservationService.pickUp(pending, null))
                .isInstanceOf(IllegalStateException.class);

        Long future = confirmedReservation(LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), branchId);
        assertThatThrownBy(() -> reservationService.pickUp(future, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("between its start and end date");
    }

    @Test
    @DisplayName("A car in the shop cannot be handed over")
    void pickUpRejectsCarInMaintenance() {
        Long id = confirmedReservation(LocalDate.now(), LocalDate.now().plusDays(1), branchId);
        // A job for another day was started early, so the car is in the shop at pick-up time
        var job = maintenanceService.schedule(scheduleRequest(LocalDate.now().plusDays(10)));
        maintenanceService.start(job.getId());

        assertThatThrownBy(() -> reservationService.pickUp(id, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAINTENANCE");
    }

    @Test
    @DisplayName("Odometer readings cannot go backwards")
    void odometerChecks() {
        Long id = confirmedReservation(LocalDate.now(), LocalDate.now().plusDays(2), branchId);

        assertThatThrownBy(() -> reservationService.pickUp(id, new ReservationPickupRequestDto(14999)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(reservationService.getById(id).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        reservationService.pickUp(id, new ReservationPickupRequestDto(15200));
        assertThatThrownBy(() -> reservationService.complete(id, new ReservationReturnRequestDto(15199)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pick-up");
    }

    @Test
    @DisplayName("A late return can still be completed")
    void lateReturn() {
        Long id = activeRental(LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        reservationService.complete(id, null);

        assertThat(reservationService.getById(id).getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    @Test
    @DisplayName("A rental that is out past its end date blocks bookings starting today, not later ones")
    void overdueRentalBlocksBookingToday() {
        activeRental(LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> reservationService.create(request(LocalDate.now(), LocalDate.now().plusDays(1), branchId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
        assertThat(reservationService.create(request(LocalDate.now().plusDays(3), LocalDate.now().plusDays(4), branchId)).getId())
                .isNotNull();
    }

    @Test
    @DisplayName("A rented car is not offered in availability search and cannot go into the shop")
    void rentedCarIsUnavailable() {
        activeRental(LocalDate.now(), LocalDate.now().plusDays(2));

        List<Long> available = carService.findAvailable(branchId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(11),
                        null, null, null, null, null, PageRequest.of(0, 100))
                .getContent().stream().map(CarListResponseDto::getId).toList();
        assertThat(available).doesNotContain(carId);

        var job = maintenanceService.schedule(scheduleRequest(LocalDate.now().plusDays(5)));
        assertThatThrownBy(() -> maintenanceService.start(job.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("out on rental");
    }

    @Test
    @DisplayName("An active rental cannot be cancelled")
    void activeRentalCannotBeCancelled() {
        Long id = activeRental(LocalDate.now(), LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> reservationService.cancel(id)).isInstanceOf(IllegalStateException.class);
    }

    private Long confirmedReservation(LocalDate start, LocalDate end, Long dropoffBranchId) {
        Long id = reservationService.create(request(start, end, dropoffBranchId)).getId();
        reservationService.confirm(id);
        return id;
    }

    /**
     * A rental out on the given dates. Pick-up has to happen within the dates, so a reservation for today is
     * picked up and then moved to the requested dates (e.g. in the past for an overdue return).
     */
    private Long activeRental(LocalDate start, LocalDate end) {
        Long id = confirmedReservation(LocalDate.now(), LocalDate.now(), branchId);
        reservationService.pickUp(id, null);
        Reservation reservation = reservationRepository.findById(id).orElseThrow();
        reservation.setStartDate(start);
        reservation.setEndDate(end);
        reservationRepository.saveAndFlush(reservation);
        return id;
    }

    private ReservationRequestDto request(LocalDate start, LocalDate end, Long dropoffBranchId) {
        return new ReservationRequestDto(customerId, carId, start, end, branchId, dropoffBranchId, "test");
    }

    private MaintenanceScheduleRequestDto scheduleRequest(LocalDate date) {
        MaintenanceScheduleRequestDto request = new MaintenanceScheduleRequestDto();
        request.setCarId(carId);
        request.setType(MaintenanceType.ROUTINE);
        request.setDescription("Oil change");
        request.setScheduledDate(date);
        return request;
    }

    private static BranchRequestDto branch(String name, String email) {
        return new BranchRequestDto(name, "1 Main St", "Gotham", "USA", "+1999888777", email, "24/7", true);
    }
}
