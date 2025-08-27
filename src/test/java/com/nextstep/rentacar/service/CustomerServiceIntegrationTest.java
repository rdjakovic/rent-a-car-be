package com.nextstep.rentacar.service;

import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.response.CustomerResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceIntegrationTest {

    @Autowired private CustomerService customerService;

    private CustomerRequestDto validCustomer(String email, String license) {
        CustomerRequestDto dto = new CustomerRequestDto();
        dto.setFirstName("Alice");
        dto.setLastName("Wonder");
        dto.setEmail(email);
        dto.setPhone("+11111111111");
        dto.setDriverLicenseNo(license);
        dto.setDateOfBirth(LocalDate.now().minusYears(22));
        dto.setAddress("42 Fantasy Rd");
        dto.setCity("Storyville");
        dto.setCountry("USA");
        dto.setLicenseExpiryDate(LocalDate.now().plusYears(3));
        return dto;
    }

    @Test
    @DisplayName("Customer CRUD, uniqueness checks and age validation")
    void customerCrudAndValidations() {
        // Underage
        CustomerRequestDto underage = validCustomer("teen@example.com", "L11111");
        underage.setDateOfBirth(LocalDate.now().minusYears(16));
        assertThatThrownBy(() -> customerService.create(underage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("18");

        // Create valid
        CustomerResponseDto c1 = customerService.create(validCustomer("alice@example.com", "L22222"));
        assertThat(c1.getId()).isNotNull();

        // Duplicate email
        assertThatThrownBy(() -> customerService.create(validCustomer("alice@example.com", "L33333")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");

        // Update
        CustomerRequestDto upd = validCustomer("alice.updated@example.com", "L22222");
        CustomerResponseDto updated = customerService.update(c1.getId(), upd);
        assertThat(updated.getEmail()).isEqualTo("alice.updated@example.com");

        // List/Search
        Page<CustomerResponseDto> list = customerService.list(PageRequest.of(0, 10));
        assertThat(list.getTotalElements()).isGreaterThanOrEqualTo(1);

        Page<CustomerResponseDto> search = customerService.search("updated", null, null, null, PageRequest.of(0, 10));
        assertThat(search.getTotalElements()).isGreaterThanOrEqualTo(1);

        // Delete
        customerService.delete(c1.getId());
        assertThatThrownBy(() -> customerService.getById(c1.getId()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    @DisplayName("searchAny returns customers matching email, firstName, lastName, or city (case-insensitive)")
    void searchAny_matchesAnyField_caseInsensitive() {
        customerService.create(validCustomer("bob@example.com", "L44444"));
        CustomerRequestDto dto2 = validCustomer("eve@example.com", "L55555");
        dto2.setFirstName("Eve");
        dto2.setLastName("Smith");
        dto2.setCity("Metropolis");
        customerService.create(dto2);

        // Match by email
        Page<CustomerResponseDto> byEmail = customerService.searchAny("bob@EXAMPLE.com", PageRequest.of(0, 10));
        assertThat(byEmail.getContent()).hasSize(1);
        assertThat(byEmail.getContent().get(0).getEmail()).isEqualTo("bob@example.com");

        // Match by firstName
        Page<CustomerResponseDto> byFirstName = customerService.searchAny("eve", PageRequest.of(0, 10));
        assertThat(byFirstName.getContent()).hasSize(1);
        assertThat(byFirstName.getContent().get(0).getFirstName()).isEqualTo("Eve");

        // Match by lastName
        Page<CustomerResponseDto> byLastName = customerService.searchAny("smith", PageRequest.of(0, 10));
        assertThat(byLastName.getContent()).hasSize(1);
        assertThat(byLastName.getContent().get(0).getLastName()).isEqualTo("Smith");

        // Match by city
        Page<CustomerResponseDto> byCity = customerService.searchAny("metropolis", PageRequest.of(0, 10));
        assertThat(byCity.getContent()).hasSize(1);
        assertThat(byCity.getContent().get(0).getCity()).isEqualTo("Metropolis");

        // No match
        Page<CustomerResponseDto> noMatch = customerService.searchAny("notfound", PageRequest.of(0, 10));
        assertThat(noMatch.getContent()).isEmpty();
    }

    @Test
    @DisplayName("searchAny supports pagination")
    void searchAny_pagination() {
        for (int i = 0; i < 15; i++) {
            CustomerRequestDto dto = validCustomer("user" + i + "@test.com", "L9" + i);
            dto.setFirstName("User" + i);
            customerService.create(dto);
        }
        Page<CustomerResponseDto> page1 = customerService.searchAny("user", PageRequest.of(0, 10));
        Page<CustomerResponseDto> page2 = customerService.searchAny("user", PageRequest.of(1, 10));
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
    }
}
