package com.nextstep.rentacar.testutil;

import com.nextstep.rentacar.domain.entity.*;
import com.nextstep.rentacar.domain.enums.*;
import com.nextstep.rentacar.dto.request.*;
import com.nextstep.rentacar.dto.response.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static com.nextstep.rentacar.testutil.builders.BranchTestDataBuilder.aBranch;
import static com.nextstep.rentacar.testutil.builders.CarTestDataBuilder.aCar;
import static com.nextstep.rentacar.testutil.builders.CustomerTestDataBuilder.aCustomer;
import static com.nextstep.rentacar.testutil.builders.ReservationTestDataBuilder.aReservation;

/**
 * Factory class for creating test data.
 * Provides convenient methods to create various test entities, DTOs, and scenarios.
 */
public class TestDataFactory {

    private static long idCounter = 1;

    // ========== Branch Factory Methods ==========
    
    public static BranchRequestDto createBranchRequest() {
        return aBranch().build();
    }

    public static BranchRequestDto createBranchRequest(String city) {
        return aBranch().withCity(city).build();
    }

    public static BranchResponseDto createBranchResponse(Long id, String name, String city) {
        BranchResponseDto response = new BranchResponseDto();
        response.setId(id);
        response.setName(name);
        response.setAddress("123 Test St");
        response.setCity(city);
        response.setCountry("USA");
        response.setPhone("+1-555-0100");
        response.setEmail("branch@test.com");
        response.setOpeningHours("9:00-18:00");
        response.setActive(true);
        return response;
    }

    public static Branch createBranchEntity() {
        Branch branch = new Branch();
        branch.setId(nextId());
        branch.setName("Test Branch");
        branch.setAddress("123 Test St");
        branch.setCity("Test City");
        branch.setCountry("Test Country");
        branch.setPhone("+1-555-0100");
        branch.setEmail("branch@test.com");
        branch.setOpeningHours("9:00-18:00");
        return branch;
    }

    // ========== Car Factory Methods ==========
    
    public static CarRequestDto createCarRequest(Long branchId) {
        return aCar().withBranchId(branchId).build();
    }

    public static CarRequestDto createLuxuryCarRequest(Long branchId) {
        return aCar().asLuxury().withBranchId(branchId).build();
    }

    public static CarRequestDto createEconomyCarRequest(Long branchId) {
        return aCar().asEconomy().withBranchId(branchId).build();
    }

    public static CarResponseDto createCarResponse(Long id, String make, String model) {
        CarResponseDto response = new CarResponseDto();
        response.setId(id);
        response.setVin("VIN" + id);
        response.setMake(make);
        response.setModel(model);
        response.setYear(2022);
        response.setCategory(CarCategory.COMPACT);
        response.setTransmission(TransmissionType.AUTOMATIC);
        response.setFuelType(FuelType.GASOLINE);
        response.setSeats(5);
        response.setMileage(10000);
        response.setDailyPrice(new BigDecimal("49.99"));
        response.setStatus(CarStatus.AVAILABLE);
        return response;
    }

    public static Car createCarEntity(Branch branch) {
        Car car = new Car();
        car.setId(nextId());
        car.setVin("VIN" + car.getId());
        car.setMake("Toyota");
        car.setModel("Corolla");
        car.setYear(2022);
        car.setCategory(CarCategory.COMPACT);
        car.setTransmission(TransmissionType.AUTOMATIC);
        car.setFuelType(FuelType.GASOLINE);
        car.setSeats(5);
        car.setMileage(10000);
        car.setDailyPrice(new BigDecimal("49.99"));
        car.setStatus(CarStatus.AVAILABLE);
        car.setBranch(branch);
        car.setDeleted(false);
        return car;
    }

    // ========== Customer Factory Methods ==========
    
    public static CustomerRequestDto createCustomerRequest() {
        return aCustomer().build();
    }

    public static CustomerRequestDto createCustomerRequest(String email) {
        return aCustomer().withEmail(email).build();
    }

    public static CustomerResponseDto createCustomerResponse(Long id, String firstName, String lastName) {
        CustomerResponseDto response = new CustomerResponseDto();
        response.setId(id);
        response.setFirstName(firstName);
        response.setLastName(lastName);
        response.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com");
        response.setPhone("+1-555-0100");
        response.setDriverLicenseNo("DL" + id);
        response.setDateOfBirth(LocalDate.now().minusYears(30));
        response.setAddress("123 Test St");
        response.setCity("Test City");
        response.setCountry("USA");
        response.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        return response;
    }

    public static Customer createCustomerEntity() {
        Customer customer = new Customer();
        customer.setId(nextId());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@test.com");
        customer.setPhone("+1-555-0100");
        customer.setDriverLicenseNo("DL" + customer.getId());
        customer.setDateOfBirth(LocalDate.now().minusYears(30));
        customer.setAddress("123 Test St");
        customer.setCity("Test City");
        customer.setCountry("USA");
        customer.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        return customer;
    }

    // ========== Reservation Factory Methods ==========
    
    public static ReservationRequestDto createReservationRequest(Long customerId, Long carId, Long branchId) {
        return aReservation()
                .forCustomer(customerId)
                .forCar(carId)
                .withSameBranch(branchId)
                .build();
    }

    public static ReservationResponseDto createReservationResponse(Long id, Long customerId, Long carId) {
        ReservationResponseDto response = new ReservationResponseDto();
        response.setId(id);
        response.setStartDate(LocalDate.now().plusDays(7));
        response.setEndDate(LocalDate.now().plusDays(14));
        response.setTotalPrice(new BigDecimal("349.93")); // 7 days * 49.99
        response.setStatus(ReservationStatus.PENDING);
        response.setNotes("Test reservation");
        return response;
    }

    public static Reservation createReservationEntity(Customer customer, Car car, Branch branch) {
        Reservation reservation = new Reservation();
        reservation.setId(nextId());
        reservation.setCustomer(customer);
        reservation.setCar(car);
        reservation.setStartDate(LocalDate.now().plusDays(7));
        reservation.setEndDate(LocalDate.now().plusDays(14));
        reservation.setPickupBranch(branch);
        reservation.setDropoffBranch(branch);
        reservation.setTotalPrice(new BigDecimal("349.93"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setNotes("Test reservation");
        return reservation;
    }

    // ========== User Factory Methods ==========
    
    public static User createUserEntity(String username, Role role) {
        User user = new User();
        user.setId(nextId());
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("$2a$10$encrypted-password"); // BCrypt encrypted
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    public static Role createRoleEntity(String name) {
        Role role = new Role();
        role.setId(nextId());
        role.setName(name);
        return role;
    }

    public static RegisterRequestDto createRegisterRequest() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("testuser" + System.currentTimeMillis());
        request.setEmail("testuser@test.com");
        request.setPassword("Test123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPhone("+1-555-0100");
        return request;
    }

    public static LoginRequestDto createLoginRequest(String email, String password) {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    // ========== Maintenance Factory Methods ==========
    
    public static MaintenanceScheduleRequestDto createMaintenanceRequest(Long carId) {
        MaintenanceScheduleRequestDto request = new MaintenanceScheduleRequestDto();
        request.setCarId(carId);
        request.setType(MaintenanceType.ROUTINE);
        request.setDescription("Regular maintenance");
        request.setScheduledDate(LocalDate.now().plusDays(7));
        return request;
    }

    public static Maintenance createMaintenanceEntity(Car car, Employee employee) {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(nextId());
        maintenance.setCar(car);
        maintenance.setEmployee(employee);
        maintenance.setMaintenanceType(MaintenanceType.ROUTINE);
        maintenance.setDescription("Regular maintenance");
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        maintenance.setScheduledDate(LocalDate.now().plusDays(7));
        // maintenance does not have setBranch method
        return maintenance;
    }

    // ========== Employee Factory Methods ==========
    
    public static Employee createEmployeeEntity(Branch branch) {
        // Create user first
        User user = new User();
        user.setId(nextId());
        user.setUsername("employee" + user.getId());
        user.setEmail("jane.smith@test.com");
        user.setPassword("$2a$10$encrypted-password");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setPhone("+1-555-0200");
        user.setEnabled(true);
        
        Employee employee = new Employee();
        employee.setId(nextId());
        employee.setUser(user);
        employee.setEmployeeId("EMP" + employee.getId());
        employee.setBranch(branch);
        employee.setPosition("Manager");
        employee.setHireDate(LocalDate.now().minusYears(2));
        employee.setIsActive(true);
        return employee;
    }

    // ========== Test Scenarios ==========
    
    /**
     * Creates a complete test scenario with branch, cars, customers, and reservations
     */
    public static class TestScenario {
        public Branch branch;
        public Car economyCar;
        public Car luxuryCar;
        public Car suvCar;
        public Customer customer1;
        public Customer customer2;
        public Reservation activeReservation;
        public Employee employee;

        public static TestScenario createFullScenario() {
            TestScenario scenario = new TestScenario();
            
            // Create branch
            scenario.branch = createBranchEntity();
            
            // Create cars
            scenario.economyCar = createCarEntity(scenario.branch);
            scenario.economyCar.setCategory(CarCategory.ECONOMY);
            scenario.economyCar.setDailyPrice(new BigDecimal("29.99"));
            
            scenario.luxuryCar = createCarEntity(scenario.branch);
            scenario.luxuryCar.setCategory(CarCategory.LUXURY);
            scenario.luxuryCar.setMake("Mercedes");
            scenario.luxuryCar.setModel("S-Class");
            scenario.luxuryCar.setDailyPrice(new BigDecimal("199.99"));
            
            scenario.suvCar = createCarEntity(scenario.branch);
            scenario.suvCar.setCategory(CarCategory.SUV);
            scenario.suvCar.setMake("Ford");
            scenario.suvCar.setModel("Explorer");
            scenario.suvCar.setSeats(7);
            scenario.suvCar.setDailyPrice(new BigDecimal("89.99"));
            
            // Create customers
            scenario.customer1 = createCustomerEntity();
            scenario.customer2 = createCustomerEntity();
            scenario.customer2.setEmail("jane.doe@test.com");
            scenario.customer2.setFirstName("Jane");
            
            // Create employee
            scenario.employee = createEmployeeEntity(scenario.branch);
            
            // Create active reservation
            scenario.activeReservation = createReservationEntity(
                scenario.customer1,
                scenario.economyCar,
                scenario.branch
            );
            scenario.activeReservation.setStatus(ReservationStatus.CONFIRMED);
            
            return scenario;
        }
    }

    // ========== Helper Methods ==========
    
    private static Long nextId() {
        return idCounter++;
    }

    public static void resetIdCounter() {
        idCounter = 1;
    }
}
