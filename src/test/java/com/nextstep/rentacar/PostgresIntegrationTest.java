package com.nextstep.rentacar;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.MileageUnit;
import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.MaintenanceRepository;
import com.nextstep.rentacar.repository.PaymentRepository;
import com.nextstep.rentacar.service.ReservationService;
import com.nextstep.rentacar.repository.ReservationRepository;
import com.nextstep.rentacar.service.CarService;
import com.nextstep.rentacar.service.SettingsService;
import com.nextstep.rentacar.dto.request.CarFilterDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the application with the prod profile against a real PostgreSQL:
 * Flyway migrations must apply and Hibernate schema validation must pass.
 * Skipped when Docker is not available.
 */
@SpringBootTest
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PostgresIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // prod has no default JWT secret (JWT_SECRET is required)
        registry.add("app.jwt.secret", () -> "postgres-it-secret-key-at-least-32-characters-long");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private CarService carService;

    @Test
    @DisplayName("All Flyway migrations apply successfully on PostgreSQL")
    void flywayMigrationsApplied() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7");
    }

    @Test
    @DisplayName("Settings are seeded with kilometers as the mileage unit (V5)")
    void settingsSeeded() {
        assertThat(settingsService.get().getMileageUnit()).isEqualTo(MileageUnit.KILOMETERS);
    }

    @Test
    @DisplayName("Reservation date check allows same-day rentals but not end before start (V4)")
    void reservationDateCheck() {
        Long id = jdbcTemplate.queryForObject("SELECT MIN(id) FROM reservations", Long.class);

        jdbcTemplate.update("UPDATE reservations SET end_date = start_date WHERE id = ?", id);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE reservations SET end_date = start_date - 1 WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Reservation status check allows ACTIVE, not unknown statuses (V7)")
    void reservationStatusCheck() {
        Long id = jdbcTemplate.queryForObject("SELECT MIN(id) FROM reservations", Long.class);

        jdbcTemplate.update("UPDATE reservations SET status = 'ACTIVE', mileage_out = 100, picked_up_at = CURRENT_TIMESTAMP WHERE id = ?", id);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE reservations SET status = 'BOGUS' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Return odometer cannot be below the pick-up odometer (V7)")
    void reservationMileageCheck() {
        Long id = jdbcTemplate.queryForObject("SELECT MIN(id) FROM reservations", Long.class);
        jdbcTemplate.update("UPDATE reservations SET mileage_out = 100 WHERE id = ?", id);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE reservations SET mileage_in = 99 WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Enum columns round-trip through PostgreSQL check constraints")
    void enumRoundTrip() {
        Car car = carRepository.findAll().getFirst();
        car.setStatus(CarStatus.MAINTENANCE);
        carRepository.saveAndFlush(car);

        assertThat(carRepository.findById(car.getId())).get()
                .extracting(Car::getStatus).isEqualTo(CarStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("Reservation search query runs on PostgreSQL")
    void reservationSearch() {
        Page<Reservation> results = reservationRepository.findBySearchTerm("alice", PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty()
                .allSatisfy(r -> assertThat(r.getCustomer().getFirstName()).isEqualTo("Alice"));
    }

    @Test
    @DisplayName("Car search (make, model, 'year make model') runs on PostgreSQL")
    void carSearch() {
        CarFilterDto filter = new CarFilterDto();
        filter.setSearch("2023 TOYOTA cam");

        assertThat(carService.list(filter, PageRequest.of(0, 10)).getContent())
                .isNotEmpty()
                .allSatisfy(car -> assertThat(car.getDisplayName()).isEqualToIgnoringCase("2023 Toyota Camry"));
    }

    @Test
    @DisplayName("Maintenance-aware availability and due-for-service queries run on PostgreSQL")
    void maintenanceQueries() {
        // V2 seeds a SCHEDULED job for car 1 (branch 1) 30 days from now
        LocalDate serviceDay = LocalDate.now().plusDays(30);
        Long carWithJob = jdbcTemplate.queryForObject(
                "SELECT car_id FROM maintenance WHERE status = 'SCHEDULED' ORDER BY id LIMIT 1", Long.class);
        Long branchId = carRepository.findById(carWithJob).orElseThrow().getBranch().getId();

        assertThat(carRepository.findAvailableCarsWithFilters(branchId, serviceDay, serviceDay,
                null, null, null, null, null, PageRequest.of(0, 50)).getContent())
                .isNotEmpty()
                .extracting(Car::getId).doesNotContain(carWithJob);

        Car due = carRepository.findAll().stream().filter(c -> !c.getId().equals(carWithJob)).findFirst().orElseThrow();
        due.setNextServiceDate(LocalDate.now().minusDays(1));
        carRepository.saveAndFlush(due);
        assertThat(carRepository.findCarsDueForServiceWithoutOpenMaintenance(LocalDate.now()))
                .extracting(Car::getId).contains(due.getId());
    }

    @Test
    @DisplayName("Maintenance list date filters run on PostgreSQL (one date, both dates, none)")
    void maintenanceDateFilters() {
        LocalDate today = LocalDate.now();
        PageRequest page = PageRequest.of(0, 10);

        assertThat(maintenanceRepository.findWithFilters(null, null, MaintenanceStatus.SCHEDULED, null, null,
                null, today.minusDays(1), page)).isNotNull();
        assertThat(maintenanceRepository.findWithFilters(null, null, MaintenanceStatus.SCHEDULED, null, null,
                today, today.plusDays(60), page).getContent()).isNotEmpty();
        assertThat(maintenanceRepository.findWithFilters(null, null, null, null, null,
                today, null, page).getContent()).isNotEmpty();
        assertThat(maintenanceRepository.findWithFilters(null, null, null, null, null,
                null, null, page).getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Reservation list date filters run on PostgreSQL (with and without search)")
    void reservationDateFilters() {
        LocalDate today = LocalDate.now();
        PageRequest page = PageRequest.of(0, 10);

        assertThat(reservationRepository.findWithFilters(null, null, null, null, today, null, page).getContent()).isNotEmpty();
        assertThat(reservationRepository.findWithFilters(null, null, null, null, null, today.plusDays(60), page).getContent()).isNotEmpty();
        assertThat(reservationRepository.findWithFiltersAndSearch(null, null, null, null, today, today.plusDays(60), null, page)
                .getContent()).isNotEmpty();
        assertThat(reservationService.listWithFilters(null, null, null, null, today, today.plusDays(60), "alice", page)
                .getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Payment list date filters run on PostgreSQL")
    void paymentDateFilters() {
        assertThat(paymentRepository.findWithFilters(null, null, null, null, null, null,
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), PageRequest.of(0, 10)).getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Next-booking query for the car list runs on PostgreSQL")
    void upcomingReservationsForCars() {
        List<Long> carIds = carRepository.findAll().stream().map(Car::getId).toList();

        assertThat(reservationRepository.findUpcomingForCars(carIds, LocalDate.now())).isNotEmpty()
                .allSatisfy(r -> assertThat(r.getEndDate()).isAfterOrEqualTo(LocalDate.now()));
        assertThat(carService.list(new CarFilterDto(), PageRequest.of(0, 20)).getContent())
                .anySatisfy(car -> assertThat(car.getNextReservation()).isNotNull());
    }

    @Test
    @DisplayName("Native soft-delete queries run on PostgreSQL")
    void nativeCarQueries() {
        Car car = carRepository.findAll().getFirst();
        car.setDeleted(true);
        carRepository.saveAndFlush(car);

        assertThat(carRepository.findByIdIncludingDeleted(car.getId())).isPresent();
        assertThat(carRepository.findDeletedCars(PageRequest.of(0, 10)).getContent())
                .extracting(Car::getId).contains(car.getId());
    }
}
