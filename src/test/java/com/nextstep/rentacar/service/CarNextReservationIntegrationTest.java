package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.request.CarFilterDto;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;

/** The car list and details show the car's current or next open booking. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CarNextReservationIntegrationTest {

    @Autowired private CarService carService;
    @Autowired private BranchService branchService;
    @Autowired private CustomerService customerService;
    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;

    private Long branchId;
    private Long carId;
    private Long otherCarId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        branchId = branchService.create(new BranchRequestDto("Harbor", "1 Dock St", "Gotham", "USA",
                "+1999888777", "harbor@example.com", "24/7", true)).getId();
        carId = createCar("WDBRF61JX1F111111", "NEXT-001", "Zephyr");
        otherCarId = createCar("WDBRF61JX1F222222", "NEXT-002", "Zephyr");
        customerId = customerService.create(new CustomerRequestDto("Nina", "Next", "nina.next@example.com",
                "+14443339999", "N1234567", LocalDate.now().minusYears(30), "2 Ave", "Gotham", "USA",
                LocalDate.now().plusYears(3))).getId();
    }

    @Test
    @DisplayName("The earliest open booking that has not ended is the next one; other cars have none")
    void nextBookingInListAndDetails() {
        Long later = book(LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        Long sooner = book(LocalDate.now().plusDays(3), LocalDate.now().plusDays(4));
        reservationService.confirm(sooner);

        var details = carService.getById(carId).getNextReservation();
        assertThat(details.getId()).isEqualTo(sooner);
        assertThat(details.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(details.getStartDate()).isEqualTo(LocalDate.now().plusDays(3));

        var listed = listZephyrs();
        assertThat(listed.stream().filter(c -> c.getId().equals(carId)).findFirst().orElseThrow()
                .getNextReservation().getId()).isEqualTo(sooner);
        assertThat(listed.stream().filter(c -> c.getId().equals(otherCarId)).findFirst().orElseThrow()
                .getNextReservation()).isNull();

        // Once the sooner one is cancelled, the later one is next
        reservationService.cancel(sooner);
        assertThat(carService.getById(carId).getNextReservation().getId()).isEqualTo(later);
    }

    @Test
    @DisplayName("A booking covering today counts; ended and closed ones do not")
    void onlyOpenBookingsThatHaveNotEnded() {
        Long today = book(LocalDate.now(), LocalDate.now().plusDays(1));
        assertThat(carService.getById(carId).getNextReservation().getId()).isEqualTo(today);

        reservationService.cancel(today);
        Long ended = book(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        Reservation past = reservationRepository.findById(ended).orElseThrow();
        past.setStartDate(LocalDate.now().minusDays(5));
        past.setEndDate(LocalDate.now().minusDays(2));
        reservationRepository.saveAndFlush(past);

        assertThat(carService.getById(carId).getNextReservation()).isNull();
    }

    @Test
    @DisplayName("A rental still out past its end date stays the current booking")
    void overdueRentalStaysCurrent() {
        Long id = book(LocalDate.now(), LocalDate.now());
        reservationService.confirm(id);
        reservationService.pickUp(id, null);
        Reservation rental = reservationRepository.findById(id).orElseThrow();
        rental.setStartDate(LocalDate.now().minusDays(4));
        rental.setEndDate(LocalDate.now().minusDays(1));
        reservationRepository.saveAndFlush(rental);

        var next = carService.getById(carId).getNextReservation();
        assertThat(next.getId()).isEqualTo(id);
        assertThat(next.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    private java.util.List<CarListResponseDto> listZephyrs() {
        CarFilterDto filter = new CarFilterDto();
        filter.setSearch("zephyr");
        return carService.list(filter, PageRequest.of(0, 10)).getContent();
    }

    private Long book(LocalDate start, LocalDate end) {
        return reservationService.create(new ReservationRequestDto(customerId, carId, start, end, branchId, branchId, "test")).getId();
    }

    private Long createCar(String vin, String plate, String model) {
        return carService.create(new CarRequestDto(vin, "Acme", model, 2022, CarCategory.COMPACT,
                TransmissionType.AUTOMATIC, FuelType.GASOLINE, 5, 1000, CarStatus.AVAILABLE,
                new BigDecimal("40.00"), branchId, "Blue", plate, "POL-NEXT")).getId();
    }
}
