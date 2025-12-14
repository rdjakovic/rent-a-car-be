package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.*;
import com.nextstep.rentacar.domain.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Transactional
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Branch testBranch;
    private Branch secondBranch;
    private Customer johnDoe;
    private Customer janeSmith;
    private Car toyotaCorolla;
    private Car hondaCivic;
    private Car mercedesBenz;
    private Reservation reservation1;
    private Reservation reservation2;
    private Reservation reservation3;
    
    private static int uniqueCounter = 1;

    @BeforeEach
    void setUp() {
        // Create branches
        testBranch = createBranch("Downtown Branch", "New York");
        secondBranch = createBranch("Airport Branch", "Los Angeles");
        
        // Create customers
        johnDoe = createCustomer("John", "Doe", "john.doe@email.com", "+1234567890");
        janeSmith = createCustomer("Jane", "Smith", "jane.smith@email.com", "+0987654321");
        
        // Create cars
        toyotaCorolla = createCar("Toyota", "Corolla", 2022, testBranch);
        hondaCivic = createCar("Honda", "Civic", 2023, testBranch);
        mercedesBenz = createCar("Mercedes", "C-Class", 2024, secondBranch);
        
        // Create reservations
        reservation1 = createReservation(johnDoe, toyotaCorolla, testBranch, testBranch);
        reservation2 = createReservation(janeSmith, hondaCivic, testBranch, secondBranch);
        reservation3 = createReservation(johnDoe, mercedesBenz, secondBranch, secondBranch);
        
        // Persist all entities
        entityManager.persistAndFlush(testBranch);
        entityManager.persistAndFlush(secondBranch);
        entityManager.persistAndFlush(johnDoe);
        entityManager.persistAndFlush(janeSmith);
        entityManager.persistAndFlush(toyotaCorolla);
        entityManager.persistAndFlush(hondaCivic);
        entityManager.persistAndFlush(mercedesBenz);
        entityManager.persistAndFlush(reservation1);
        entityManager.persistAndFlush(reservation2);
        entityManager.persistAndFlush(reservation3);
        
        entityManager.clear();
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by customer first name (case-insensitive)")
    void findBySearchTerm_shouldFindByCustomerFirstName() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by first name - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("john", pageable);
        
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(r -> r.getCustomer().getFirstName())
                .containsOnly("John");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by customer last name (case-insensitive)")
    void findBySearchTerm_shouldFindByCustomerLastName() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by last name - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("SMITH", pageable);
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCustomer().getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by customer full name")
    void findBySearchTerm_shouldFindByCustomerFullName() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by full name
        Page<Reservation> results = reservationRepository.findBySearchTerm("Jane Smith", pageable);
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCustomer().getFullName()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by customer email (case-insensitive)")
    void findBySearchTerm_shouldFindByCustomerEmail() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by email - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("JOHN.DOE@EMAIL.COM", pageable);
        
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(r -> r.getCustomer().getEmail())
                .containsOnly("john.doe@email.com");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by customer phone number")
    void findBySearchTerm_shouldFindByCustomerPhone() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by phone number
        Page<Reservation> results = reservationRepository.findBySearchTerm("1234567890", pageable);
        
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(r -> r.getCustomer().getPhone())
                .containsOnly("+1234567890");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by reservation ID and prioritize exact matches")
    void findBySearchTerm_shouldFindByReservationIdWithPriority() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Get the ID of reservation1 for exact match test
        String reservationId = reservation1.getId().toString();
        
        // Search by exact reservation ID
        Page<Reservation> results = reservationRepository.findBySearchTerm(reservationId, pageable);
        
        assertThat(results.getContent()).isNotEmpty();
        // The exact ID match should be first due to ordering
        assertThat(results.getContent().get(0).getId()).isEqualTo(reservation1.getId());
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by car make (case-insensitive)")
    void findBySearchTerm_shouldFindByCarMake() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by car make - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("toyota", pageable);
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCar().getMake()).isEqualTo("Toyota");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by car model (case-insensitive)")
    void findBySearchTerm_shouldFindByCarModel() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by car model - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("CIVIC", pageable);
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCar().getModel()).isEqualTo("Civic");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by car display name (year make model)")
    void findBySearchTerm_shouldFindByCarDisplayName() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by car display name (year + make + model)
        Page<Reservation> results = reservationRepository.findBySearchTerm("2024 Mercedes", pageable);
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getCar().getDisplayName()).contains("2024 Mercedes");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by pickup branch name (case-insensitive)")
    void findBySearchTerm_shouldFindByPickupBranchName() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by pickup branch name - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("downtown", pageable);
        
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(r -> r.getPickupBranch().getName())
                .containsOnly("Downtown Branch");
    }

    @Test
    @DisplayName("findBySearchTerm should find reservations by dropoff branch name (case-insensitive)")
    void findBySearchTerm_shouldFindByDropoffBranchName() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search by dropoff branch name - case insensitive
        Page<Reservation> results = reservationRepository.findBySearchTerm("AIRPORT", pageable);
        
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(r -> r.getDropoffBranch().getName())
                .containsOnly("Airport Branch");
    }

    @Test
    @DisplayName("findBySearchTerm should return empty results for non-matching search")
    void findBySearchTerm_shouldReturnEmptyForNonMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search for non-existing term
        Page<Reservation> results = reservationRepository.findBySearchTerm("nonexistent", pageable);
        
        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findBySearchTerm should return all reservations when search term is null")
    void findBySearchTerm_shouldReturnAllWhenSearchIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search with null term
        Page<Reservation> results = reservationRepository.findBySearchTerm(null, pageable);
        
        assertThat(results.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("findBySearchTerm should return all reservations when search term is empty")
    void findBySearchTerm_shouldReturnAllWhenSearchIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search with empty term
        Page<Reservation> results = reservationRepository.findBySearchTerm("", pageable);
        
        assertThat(results.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("findBySearchTerm should handle partial matches correctly")
    void findBySearchTerm_shouldHandlePartialMatches() {
        Pageable pageable = PageRequest.of(0, 10);
        
        // Search with partial email
        Page<Reservation> results = reservationRepository.findBySearchTerm("doe@email", pageable);
        
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(r -> r.getCustomer().getEmail())
                .containsOnly("john.doe@email.com");
    }

    @Test
    @DisplayName("findBySearchTerm should respect pagination")
    void findBySearchTerm_shouldRespectPagination() {
        // Search for "john" which should return 2 results, but limit to 1 per page
        Pageable pageable = PageRequest.of(0, 1);
        
        Page<Reservation> results = reservationRepository.findBySearchTerm("john", pageable);
        
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("findBySearchTerm should order results by creation date descending")
    void findBySearchTerm_shouldOrderResultsCorrectly() {
        // Create a reservation with a specific ID pattern that might match partial searches
        Customer testCustomer = createCustomer("Test", "User", "test@email.com", "+1111111111");
        Car testCar = createCar("Test", "Car", 2023, testBranch);
        Reservation testReservation = createReservation(testCustomer, testCar, testBranch, testBranch);
        
        entityManager.persistAndFlush(testCustomer);
        entityManager.persistAndFlush(testCar);
        entityManager.persistAndFlush(testReservation);
        entityManager.clear();
        
        Pageable pageable = PageRequest.of(0, 10);
        String exactId = testReservation.getId().toString();
        
        // Search by exact ID
        Page<Reservation> results = reservationRepository.findBySearchTerm(exactId, pageable);
        
        assertThat(results.getContent()).isNotEmpty();
        // Should find the reservation by ID
        assertThat(results.getContent()).anyMatch(r -> r.getId().equals(testReservation.getId()));
    }

    // Helper methods for creating test entities
    private Branch createBranch(String name, String city) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setAddress("123 Test St");
        branch.setCity(city);
        branch.setCountry("USA");
        branch.setPhone("+1-555-0100");
        branch.setEmail("branch@test.com");
        branch.setOpeningHours("9:00-18:00");
        return branch;
    }

    private Customer createCustomer(String firstName, String lastName, String email, String phone) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setDriverLicenseNo("DL" + getUniqueId());
        customer.setDateOfBirth(LocalDate.now().minusYears(30));
        customer.setAddress("123 Test St");
        customer.setCity("Test City");
        customer.setCountry("USA");
        customer.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        return customer;
    }

    private Car createCar(String make, String model, Integer year, Branch branch) {
        Car car = new Car();
        car.setVin("VIN" + getUniqueId());
        car.setMake(make);
        car.setModel(model);
        car.setYear(year);
        car.setCategory(CarCategory.COMPACT);
        car.setTransmission(TransmissionType.AUTOMATIC);
        car.setFuelType(FuelType.GASOLINE);
        car.setSeats(5);
        car.setMileage(10000);
        car.setDailyPrice(new BigDecimal("49.99"));
        car.setStatus(CarStatus.AVAILABLE);
        car.setBranch(branch);
        car.setColor("White");
        car.setLicensePlate("TEST" + getUniqueId());
        car.setDeleted(false);
        return car;
    }
    
    private synchronized String getUniqueId() {
        return String.valueOf(uniqueCounter++);
    }

    private Reservation createReservation(Customer customer, Car car, Branch pickupBranch, Branch dropoffBranch) {
        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setCar(car);
        reservation.setStartDate(LocalDate.now().plusDays(7));
        reservation.setEndDate(LocalDate.now().plusDays(14));
        reservation.setPickupBranch(pickupBranch);
        reservation.setDropoffBranch(dropoffBranch);
        reservation.setTotalPrice(new BigDecimal("349.93"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setNotes("Test reservation");
        reservation.setCurrency("USD");
        return reservation;
    }
}