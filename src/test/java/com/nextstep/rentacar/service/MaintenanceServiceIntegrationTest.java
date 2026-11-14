package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceCompleteRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceUpdateRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.dto.response.MaintenanceResponseDto;
import com.nextstep.rentacar.repository.CarRepository;
import jakarta.persistence.EntityNotFoundException;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MaintenanceServiceIntegrationTest {

    @Autowired private MaintenanceService maintenanceService;
    @Autowired private CarService carService;
    @Autowired private BranchService branchService;
    @Autowired private CustomerService customerService;
    @Autowired private ReservationService reservationService;
    @Autowired private CarRepository carRepository;

    private Long branchId;
    private Long carId;

    @BeforeEach
    void setup() {
        branchId = branchService.create(new BranchRequestDto(
                "Service Hub",
                "500 Industrial Rd",
                "Central City",
                "USA",
                "+18887776666",
                "hub@example.com",
                "Mon-Fri 9-18",
                true
        )).getId();

        carId = createCar("JH4KA9650MC000001", "SERV-001", CarStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Schedule -> Start sets car to MAINTENANCE; Complete/Cancel restores to AVAILABLE")
    void maintenanceLifecycleUpdatesCarStatus() {
        var m = schedule(MaintenanceType.ROUTINE, "Oil change", LocalDate.now().plusDays(1));
        assertThat(m.getId()).isNotNull();
        assertThat(m.getStatus()).isEqualTo(MaintenanceStatus.SCHEDULED);

        var inProgress = maintenanceService.start(m.getId());
        assertThat(inProgress.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.MAINTENANCE);

        // Complete -> AVAILABLE
        var completed = maintenanceService.complete(m.getId(), null);
        assertThat(completed.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.AVAILABLE);

        // Schedule again and cancel
        var m2 = schedule(MaintenanceType.INSPECTION, "Inspection", LocalDate.now().plusDays(2));
        maintenanceService.start(m2.getId());
        var cancelled = maintenanceService.cancel(m2.getId());
        assertThat(cancelled.getStatus()).isEqualTo(MaintenanceStatus.CANCELLED);
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Completing records cost, odometer, notes and updates the car's mileage and service dates")
    void completeRecordsDetails() {
        var m = schedule(MaintenanceType.ROUTINE, "Oil change", LocalDate.now());
        maintenanceService.start(m.getId());
        LocalDate nextService = LocalDate.now().plusMonths(6);

        var completed = maintenanceService.complete(m.getId(),
                new MaintenanceCompleteRequestDto(new BigDecimal("149.90"), 21500, "Synthetic oil", nextService));

        assertThat(completed.getCost()).isEqualByComparingTo("149.90");
        assertThat(completed.getOdometerReading()).isEqualTo(21500);
        assertThat(completed.getNotes()).isEqualTo("Synthetic oil");
        assertThat(completed.getCompletedDate()).isEqualTo(LocalDate.now());

        CarResponseDto car = carService.getById(carId);
        assertThat(car.getMileage()).isEqualTo(21500);
        assertThat(car.getLastServiceDate()).isEqualTo(LocalDate.now());
        assertThat(car.getNextServiceDate()).isEqualTo(nextService);
    }

    @Test
    @DisplayName("An odometer reading below the car's mileage is rejected")
    void completeRejectsOdometerGoingBackwards() {
        var m = schedule(MaintenanceType.ROUTINE, "Oil change", LocalDate.now());
        maintenanceService.start(m.getId());

        assertThatThrownBy(() -> maintenanceService.complete(m.getId(),
                new MaintenanceCompleteRequestDto(null, 19999, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lower than the car's recorded mileage");
    }

    @Test
    @DisplayName("Cancelling a scheduled job leaves the car status alone")
    void cancelScheduledKeepsCarStatus() {
        Long rentedCarId = createCar("JH4KA9650MC000002", "SERV-002", CarStatus.RENTED);
        var m = maintenanceService.schedule(scheduleRequest(rentedCarId, MaintenanceType.ROUTINE, "Oil change", LocalDate.now().plusDays(5)));

        maintenanceService.cancel(m.getId());

        assertThat(carService.getById(rentedCarId).getStatus()).isEqualTo(CarStatus.RENTED);
    }

    @Test
    @DisplayName("Finishing one job keeps the car in the shop while another job is still in progress")
    void carStaysInMaintenanceWhileAnotherJobInProgress() {
        var first = schedule(MaintenanceType.ROUTINE, "Oil change", LocalDate.now());
        var second = schedule(MaintenanceType.REPAIR, "Brakes", LocalDate.now());
        maintenanceService.start(first.getId());
        maintenanceService.start(second.getId());

        maintenanceService.complete(first.getId(), null);
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.MAINTENANCE);

        maintenanceService.cancel(second.getId());
        assertThat(carService.getById(carId).getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Get by id returns the job; unknown id is not found")
    void getById() {
        var m = schedule(MaintenanceType.RECALL, "Airbag recall", LocalDate.now().plusDays(1));

        MaintenanceResponseDto found = maintenanceService.getById(m.getId());
        assertThat(found.getMaintenanceType()).isEqualTo(MaintenanceType.RECALL);
        assertThat(found.getCar().getId()).isEqualTo(carId);

        assertThatThrownBy(() -> maintenanceService.getById(999_999L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Only scheduled jobs can be edited; a new date cannot be in the past")
    void updateScheduledOnly() {
        var m = schedule(MaintenanceType.ROUTINE, "Oil change", LocalDate.now().plusDays(1));
        LocalDate newDate = LocalDate.now().plusDays(3);

        var updated = maintenanceService.update(m.getId(), new MaintenanceUpdateRequestDto(
                MaintenanceType.INSPECTION, "Inspection", newDate, new BigDecimal("75.00"), "Bring winter tires"));
        assertThat(updated.getMaintenanceType()).isEqualTo(MaintenanceType.INSPECTION);
        assertThat(updated.getScheduledDate()).isEqualTo(newDate);
        assertThat(updated.getCost()).isEqualByComparingTo("75.00");
        assertThat(updated.getNotes()).isEqualTo("Bring winter tires");

        assertThatThrownBy(() -> maintenanceService.update(m.getId(), new MaintenanceUpdateRequestDto(
                MaintenanceType.INSPECTION, "Inspection", LocalDate.now().minusDays(1), null, null)))
                .isInstanceOf(IllegalArgumentException.class);

        maintenanceService.start(m.getId());
        assertThatThrownBy(() -> maintenanceService.update(m.getId(), new MaintenanceUpdateRequestDto(
                MaintenanceType.INSPECTION, "Inspection", newDate, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Maintenance cannot be scheduled on a day the car is booked")
    void scheduleRejectsBookedDay() {
        LocalDate start = LocalDate.now().plusDays(10);
        reservationService.create(reservationRequest(start, start.plusDays(2)));

        assertThatThrownBy(() -> schedule(MaintenanceType.ROUTINE, "Oil change", start.plusDays(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reservation");

        var m = schedule(MaintenanceType.ROUTINE, "Oil change", start.plusDays(3));
        assertThatThrownBy(() -> maintenanceService.update(m.getId(), new MaintenanceUpdateRequestDto(
                MaintenanceType.ROUTINE, "Oil change", start, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A scheduled job blocks bookings and availability on its date")
    void scheduledJobBlocksBookings() {
        LocalDate serviceDay = LocalDate.now().plusDays(10);
        schedule(MaintenanceType.ROUTINE, "Oil change", serviceDay);

        assertThatThrownBy(() -> reservationService.create(reservationRequest(serviceDay.minusDays(1), serviceDay.plusDays(1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maintenance");
        assertThat(availableCarIds(serviceDay, serviceDay)).doesNotContain(carId);

        // Other dates are unaffected
        assertThat(reservationService.create(reservationRequest(serviceDay.plusDays(1), serviceDay.plusDays(2))).getId()).isNotNull();
        assertThat(availableCarIds(serviceDay.minusDays(3), serviceDay.minusDays(1))).contains(carId);
    }

    @Test
    @DisplayName("A job in progress blocks bookings starting today")
    void inProgressJobBlocksBookingToday() {
        var m = schedule(MaintenanceType.REPAIR, "Brakes", LocalDate.now());
        maintenanceService.start(m.getId());

        assertThatThrownBy(() -> reservationService.create(reservationRequest(LocalDate.now(), LocalDate.now().plusDays(2))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Cars due for service are listed until a job is scheduled for them")
    void carsDueForService() {
        Long dueCarId = createCar("JH4KA9650MC000003", "SERV-003", CarStatus.AVAILABLE);
        Car dueCar = carRepository.findById(dueCarId).orElseThrow();
        dueCar.setNextServiceDate(LocalDate.now().plusDays(5));
        carRepository.save(dueCar);

        assertThat(maintenanceService.findCarsDueForService(14)).extracting(CarResponseDto::getId).contains(dueCarId);
        assertThat(maintenanceService.findCarsDueForService(2)).extracting(CarResponseDto::getId).doesNotContain(dueCarId);

        maintenanceService.schedule(scheduleRequest(dueCarId, MaintenanceType.ROUTINE, "Service", LocalDate.now().plusDays(4)));
        assertThat(maintenanceService.findCarsDueForService(14)).extracting(CarResponseDto::getId).doesNotContain(dueCarId);
    }

    private MaintenanceResponseDto schedule(MaintenanceType type, String description, LocalDate date) {
        return maintenanceService.schedule(scheduleRequest(carId, type, description, date));
    }

    private static MaintenanceScheduleRequestDto scheduleRequest(Long carId, MaintenanceType type, String description, LocalDate date) {
        MaintenanceScheduleRequestDto request = new MaintenanceScheduleRequestDto();
        request.setCarId(carId);
        request.setType(type);
        request.setDescription(description);
        request.setScheduledDate(date);
        return request;
    }

    private Long createCar(String vin, String plate, CarStatus status) {
        return carService.create(carRequest(vin, plate, status)).getId();
    }

    private CarRequestDto carRequest(String vin, String plate, CarStatus status) {
        return new CarRequestDto(
                vin,
                "Honda",
                "Accord",
                2019,
                CarCategory.STANDARD,
                TransmissionType.MANUAL,
                FuelType.GASOLINE,
                5,
                20000,
                status,
                new BigDecimal("49.00"),
                branchId,
                "White",
                plate,
                "POL-1001"
        );
    }

    private ReservationRequestDto reservationRequest(LocalDate start, LocalDate end) {
        Long customerId = customerService.create(new CustomerRequestDto(
                "Sam",
                "Driver",
                "sam.driver." + start + "." + end + "@example.com",
                "+15550001111",
                "DL-" + start.toEpochDay() + "-" + end.toEpochDay(),
                LocalDate.now().minusYears(30),
                "1 Main St",
                "Central City",
                "USA",
                LocalDate.now().plusYears(3)
        )).getId();
        ReservationRequestDto request = new ReservationRequestDto();
        request.setCustomerId(customerId);
        request.setCarId(carId);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setPickupBranchId(branchId);
        request.setDropoffBranchId(branchId);
        return request;
    }

    private java.util.List<Long> availableCarIds(LocalDate start, LocalDate end) {
        return carService.findAvailable(branchId, start, end, null, null, null, null, null, PageRequest.of(0, 100))
                .getContent().stream().map(CarListResponseDto::getId).toList();
    }
}
