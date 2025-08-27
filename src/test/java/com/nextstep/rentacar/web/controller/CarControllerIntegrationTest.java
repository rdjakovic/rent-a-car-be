package com.nextstep.rentacar.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.service.BranchService;
import com.nextstep.rentacar.service.CarService;
import com.nextstep.rentacar.service.CustomerService;
import com.nextstep.rentacar.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.nextstep.rentacar.testutil.builders.BranchTestDataBuilder.aBranch;
import static com.nextstep.rentacar.testutil.builders.CarTestDataBuilder.aCar;
import static com.nextstep.rentacar.testutil.builders.CustomerTestDataBuilder.aCustomer;
import static com.nextstep.rentacar.testutil.builders.ReservationTestDataBuilder.aReservation;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarService carService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ReservationService reservationService;

    private Long branchId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        // Create test branch
        branchId = branchService.create(aBranch().inNewYork().build()).getId();
        // Create test customer
        customerId = customerService.create(aCustomer().fromNewYork().build()).getId();
    }

    @Test
    @DisplayName("GET /api/cars - should return paginated list of cars")
    @WithMockUser(roles = "ADMIN")
    void listCars_shouldReturnPaginatedResults() throws Exception {
        // Given
        carService.create(aCar().withBranchId(branchId).build());
        carService.create(aCar().asLuxury().withBranchId(branchId).build());
        carService.create(aCar().asSUV().withBranchId(branchId).build());

        // When & Then
        mockMvc.perform(get("/api/cars")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "dailyPrice,desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].dailyPrice").value(199.99));
    }

    @Test
    @DisplayName("POST /api/cars - should create new car")
    @WithMockUser(roles = "ADMIN")
    void createCar_shouldReturnCreatedCar() throws Exception {
        // Given
        CarRequestDto request = aCar()
                .withBranchId(branchId)
                .withVin("12345678901234567")
                .asElectric()
                .build();

        // When & Then
        mockMvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vin").value("12345678901234567"))
                .andExpect(jsonPath("$.make").value("Tesla"))
                .andExpect(jsonPath("$.model").value("Model 3"))
                .andExpect(jsonPath("$.fuelType").value("ELECTRIC"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/cars - should return 400 for invalid data")
    @WithMockUser(roles = "ADMIN")
    void createCar_withInvalidData_shouldReturn400() throws Exception {
        // Given - Car without required VIN
        CarRequestDto request = aCar().withBranchId(branchId).build();
        request.setVin(null);

        // When & Then
        mockMvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/cars - should return 409 for duplicate VIN")
    @WithMockUser(roles = "ADMIN")
    void createCar_withDuplicateVin_shouldReturn409() throws Exception {
        // Given
        String duplicateVin = "12345678901234568";
        carService.create(aCar().withBranchId(branchId).withVin(duplicateVin).build());
        
        CarRequestDto request = aCar()
                .withBranchId(branchId)
                .withVin(duplicateVin)
                .build();

        // When & Then
        mockMvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/cars/{id} - should return car by ID")
    @WithMockUser(roles = "EMPLOYEE")
    void getCarById_shouldReturnCar() throws Exception {
        // Given
        Long carId = carService.create(aCar().withBranchId(branchId).asHybrid().build()).getId();

        // When & Then
        mockMvc.perform(get("/api/cars/{id}", carId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(carId))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Prius"))
                .andExpect(jsonPath("$.fuelType").value("HYBRID"));
    }

    @Test
    @DisplayName("GET /api/cars/{id} - should return 404 for non-existent car")
    @WithMockUser(roles = "EMPLOYEE")
    void getCarById_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/cars/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/cars/{id} - should update car")
    @WithMockUser(roles = "ADMIN")
    void updateCar_shouldReturnUpdatedCar() throws Exception {
        // Given
        Long carId = carService.create(aCar().withBranchId(branchId).build()).getId();
        
        CarRequestDto updateRequest = aCar()
                .withBranchId(branchId)
                .withColor("Blue")
                .withDailyPrice("59.99")
                .build();

        // When & Then
        mockMvc.perform(put("/api/cars/{id}", carId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(carId))
                .andExpect(jsonPath("$.color").value("Blue"))
                .andExpect(jsonPath("$.dailyPrice").value(59.99));
    }

    @Test
    @DisplayName("DELETE /api/cars/{id} - should soft delete car")
    @WithMockUser(roles = "ADMIN")
    void deleteCar_shouldSoftDeleteCar() throws Exception {
        // Given
        Long carId = carService.create(aCar().withBranchId(branchId).build()).getId();

        // When & Then
        mockMvc.perform(delete("/api/cars/{id}", carId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify car is soft deleted (status = OUT_OF_SERVICE)
        mockMvc.perform(get("/api/cars/{id}", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
    }

    @Test
    @DisplayName("POST /api/cars/{id}/restore - should restore soft deleted car")
    @WithMockUser(roles = "ADMIN")
    void restoreCar_shouldRestoreSoftDeletedCar() throws Exception {
        // Given
        Long carId = carService.create(aCar().withBranchId(branchId).build()).getId();
        carService.delete(carId); // Soft delete first

        // When & Then
        mockMvc.perform(post("/api/cars/{id}/restore", carId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(carId))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/cars/branch/{branchId} - should return cars by branch")
    @WithMockUser(roles = "EMPLOYEE")
    void getCarsByBranch_shouldReturnFilteredCars() throws Exception {
        // Given
        Long otherBranchId = branchService.create(aBranch().inLosAngeles().build()).getId();
        
        carService.create(aCar().withBranchId(branchId).build());
        carService.create(aCar().withBranchId(branchId).asLuxury().build());
        carService.create(aCar().withBranchId(otherBranchId).asSUV().build());

        // When & Then
        mockMvc.perform(get("/api/cars/branch/{branchId}", branchId)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/cars/available - should return available cars for date range")
    @WithMockUser(roles = "CUSTOMER")
    void getAvailableCars_shouldFilterByDateAndCriteria() throws Exception {
        // Given
        Long carId1 = carService.create(aCar()
                .withBranchId(branchId)
                .asEconomy()
                .build()).getId();
        
        Long carId2 = carService.create(aCar()
                .withBranchId(branchId)
                .asLuxury()
                .build()).getId();
        
        Long carId3 = carService.create(aCar()
                .withBranchId(branchId)
                .asSUV()
                .build()).getId();

        // Create reservation for car1 to make it unavailable
        reservationService.create(aReservation()
                .forCustomer(customerId)
                .forCar(carId1)
                .withDates(LocalDate.now().plusDays(5), LocalDate.now().plusDays(10))
                .withSameBranch(branchId)
                .build());

        // When & Then - Search for available cars in the reservation period
        mockMvc.perform(get("/api/cars/available")
                .param("branchId", branchId.toString())
                .param("startDate", LocalDate.now().plusDays(7).toString())
                .param("endDate", LocalDate.now().plusDays(9).toString())
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2))) // Only car2 and car3 available
                .andExpect(jsonPath("$.totalElements").value(2));

        // Test with category filter
        mockMvc.perform(get("/api/cars/available")
                .param("branchId", branchId.toString())
                .param("startDate", LocalDate.now().plusDays(7).toString())
                .param("endDate", LocalDate.now().plusDays(9).toString())
                .param("category", "LUXURY")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("LUXURY"));
    }

    @Test
    @DisplayName("GET /api/cars/available - with price filter")
    @WithMockUser(roles = "CUSTOMER")
    void getAvailableCars_withPriceFilter_shouldReturnFilteredCars() throws Exception {
        // Given
        carService.create(aCar().withBranchId(branchId).asEconomy().withDailyPrice("29.99").build());
        carService.create(aCar().withBranchId(branchId).withDailyPrice("49.99").build());
        carService.create(aCar().withBranchId(branchId).asLuxury().withDailyPrice("199.99").build());

        // When & Then - Search with max price filter
        mockMvc.perform(get("/api/cars/available")
                .param("branchId", branchId.toString())
                .param("startDate", LocalDate.now().plusDays(1).toString())
                .param("endDate", LocalDate.now().plusDays(3).toString())
                .param("maxPrice", "50.00")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].dailyPrice", everyItem(lessThanOrEqualTo(50.00))));
    }

    @Test
    @DisplayName("GET /api/cars/deleted - should return only deleted cars")
    @WithMockUser(roles = "ADMIN")
    void getDeletedCars_shouldReturnOnlyDeletedCars() throws Exception {
        // Given
        Long activeCarId = carService.create(aCar().withBranchId(branchId).build()).getId();
        Long deletedCarId1 = carService.create(aCar().withBranchId(branchId).asLuxury().build()).getId();
        Long deletedCarId2 = carService.create(aCar().withBranchId(branchId).asSUV().build()).getId();
        
        carService.delete(deletedCarId1);
        carService.delete(deletedCarId2);

        // When & Then
        mockMvc.perform(get("/api/cars/deleted")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("OUT_OF_SERVICE"))));
    }

    @Test
    @DisplayName("Security - Unauthorized access should return 403")
    void accessWithoutAuthentication_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/cars"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security - Customer cannot create cars")
    @WithMockUser(roles = "CUSTOMER")
    void createCar_asCustomer_shouldReturn403() throws Exception {
        CarRequestDto request = aCar().withBranchId(branchId).build();

        mockMvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security - Employee can create but not delete cars")
    @WithMockUser(roles = "EMPLOYEE")
    void deleteCar_asEmployee_shouldReturn403() throws Exception {
        Long carId = carService.create(aCar().withBranchId(branchId).build()).getId();

        mockMvc.perform(delete("/api/cars/{id}", carId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
