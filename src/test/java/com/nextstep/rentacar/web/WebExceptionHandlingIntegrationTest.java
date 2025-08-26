package com.nextstep.rentacar.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.rentacar.domain.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class WebExceptionHandlingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("404 -> ProblemDetail when entity not found")
    void notFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/api/cars/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail", containsString("Car not found")))
                .andExpect(jsonPath("$.path").value("/api/cars/999999"));
    }

    @Test
    @DisplayName("400 -> ProblemDetail for invalid request body validation errors")
    void validationErrorsProblemDetail() throws Exception {
        // Empty body for customer -> many @NotBlank/@NotNull fail
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.errors", iterableWithSize(greaterThan(0))))
                .andExpect(jsonPath("$.errors[*].field", hasItem("firstName")));
    }

    @Test
    @DisplayName("400 -> ProblemDetail for invalid date range (IllegalArgumentException)")
    void illegalArgumentProblemDetail() throws Exception {
        mockMvc.perform(get("/api/cars/available")
                        .param("branchId", "1")
                        .param("startDate", LocalDate.now().plusDays(3).toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail", containsString("Invalid date range")))
                .andExpect(jsonPath("$.path", containsString("/api/cars/available")));
    }

    @Test
    @DisplayName("409 -> ProblemDetail for illegal state (completing non-confirmed reservation)")
    void illegalStateProblemDetail() throws Exception {
        // Create branch
        var branchBody = Map.of(
                "name", "Test Branch",
                "address", "1 St",
                "city", "City",
                "country", "US",
                "phone", "+11111111111",
                "email", "b@test.com",
                "openingHours", "9-5",
                "active", true
        );
        String branchJson = objectMapper.writeValueAsString(branchBody);
        Long branchId = objectMapper.readTree(
                mockMvc.perform(post("/api/branches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(branchJson))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray()
        ).get("id").asLong();

        // Create car
        var carBody = Map.<String, Object>ofEntries(
                Map.entry("vin", "1HGCM82633A004352"),
                Map.entry("make", "Make"),
                Map.entry("model", "Model"),
                Map.entry("year", 2020),
                Map.entry("category", CarCategory.COMPACT.name()),
                Map.entry("transmission", TransmissionType.AUTOMATIC.name()),
                Map.entry("fuelType", FuelType.GASOLINE.name()),
                Map.entry("seats", 4),
                Map.entry("mileage", 1000),
                Map.entry("dailyPrice", new BigDecimal("10.00")),
                Map.entry("branchId", branchId),
                Map.entry("color", "Blue"),
                Map.entry("licensePlate", "PLATE"),
                Map.entry("insurancePolicy", "POL")
        );
        Long carId = objectMapper.readTree(
                mockMvc.perform(post("/api/cars")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(carBody)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray()
        ).get("id").asLong();

        // Create customer
        var customerBody = Map.of(
                "firstName", "Jane",
                "lastName", "Doe",
                "email", "jane.doe@example.com",
                "phone", "+12223334444",
                "driverLicenseNo", "D0001",
                "dateOfBirth", LocalDate.now().minusYears(25).toString(),
                "address", "Addr",
                "city", "City",
                "country", "US",
                "licenseExpiryDate", LocalDate.now().plusYears(2).toString()
        );
        Long customerId = objectMapper.readTree(
                mockMvc.perform(post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(customerBody)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray()
        ).get("id").asLong();

        // Create reservation (status PENDING)
        var reservationBody = Map.of(
                "customerId", customerId,
                "carId", carId,
                "startDate", LocalDate.now().plusDays(2).toString(),
                "endDate", LocalDate.now().plusDays(5).toString(),
                "pickupBranchId", branchId,
                "dropoffBranchId", branchId,
                "notes", "test"
        );
        Long reservationId = objectMapper.readTree(
                mockMvc.perform(post("/api/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reservationBody)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray()
        ).get("id").asLong();

        // Attempt to complete while still PENDING -> IllegalStateException -> 409
        mockMvc.perform(post("/api/reservations/{id}/complete", reservationId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail", containsString("cannot be completed")))
                .andExpect(jsonPath("$.path", containsString("/api/reservations/" + reservationId + "/complete")));
    }
}
