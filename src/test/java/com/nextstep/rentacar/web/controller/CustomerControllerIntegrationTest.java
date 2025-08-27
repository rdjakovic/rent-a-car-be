package com.nextstep.rentacar.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.mapper.CustomerMapper;
import com.nextstep.rentacar.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired CustomerRepository customerRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired CustomerMapper customerMapper;

    @BeforeEach
    void setup() {
        customerRepository.deleteAll();
    }

    private CustomerRequestDto validCustomer(String email, String license) {
        CustomerRequestDto dto = new CustomerRequestDto();
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setEmail(email);
        dto.setPhone("+1234567890");
        dto.setDriverLicenseNo(license);
        dto.setDateOfBirth(LocalDate.now().minusYears(30));
        dto.setAddress("123 Main St");
        dto.setCity("Testville");
        dto.setCountry("Testland");
        dto.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        return dto;
    }

    @Test
    @DisplayName("/api/customers/searchany returns customers matching any field")
    void searchAny_endpoint_returnsMatches() throws Exception {
        // Insert customers
        var c1 = validCustomer("john@example.com", "L100");
        c1.setFirstName("John");
        c1.setLastName("Doe");
        c1.setCity("Springfield");
        customerRepository.save(customerMapper.toEntity(c1));

        var c2 = validCustomer("jane@example.com", "L101");
        c2.setFirstName("Jane");
        c2.setLastName("Smith");
        c2.setCity("Metropolis");
        customerRepository.save(customerMapper.toEntity(c2));

        // Match by email
        mockMvc.perform(get("/api/customers/searchany")
                .param("search", "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"));

        // Match by firstName
        mockMvc.perform(get("/api/customers/searchany")
                .param("search", "Jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Jane"));

        // Match by lastName
        mockMvc.perform(get("/api/customers/searchany")
                .param("search", "Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Smith"));

        // Match by city
        mockMvc.perform(get("/api/customers/searchany")
                .param("search", "Metropolis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].city").value("Metropolis"));

        // No match
        mockMvc.perform(get("/api/customers/searchany")
                .param("search", "notfound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
