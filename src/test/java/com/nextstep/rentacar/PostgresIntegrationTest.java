package com.nextstep.rentacar;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.ReservationRepository;
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

    @Test
    @DisplayName("All Flyway migrations apply successfully on PostgreSQL")
    void flywayMigrationsApplied() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4");
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
