package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class CustomerRepositoryTest {
    @Autowired CustomerRepository customerRepository;

    private Customer customer(String email, String firstName, String lastName, String city, String license) {
        Customer c = new Customer();
        c.setEmail(email);
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setCity(city);
        c.setDriverLicenseNo(license);
        c.setDateOfBirth(LocalDate.now().minusYears(25));
        c.setPhone("+1234567890");
        c.setAddress("123 Main St");
        c.setCountry("Testland");
        c.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        return c;
    }

    @Test
    @DisplayName("searchAny matches by email, firstName, lastName, or city (case-insensitive)")
    void searchAny_matchesAnyField() {
        customerRepository.save(customer("a@a.com", "Alpha", "Bravo", "XCity", "L1"));
        customerRepository.save(customer("b@b.com", "Beta", "Charlie", "YTown", "L2"));
        customerRepository.save(customer("c@c.com", "Gamma", "Delta", "ZVille", "L3"));

        // Match by email
        Page<Customer> byEmail = customerRepository.searchAny("A@A.COM", PageRequest.of(0, 10));
        assertThat(byEmail.getContent()).hasSize(1);
        assertThat(byEmail.getContent().get(0).getEmail()).isEqualTo("a@a.com");

        // Match by firstName
        Page<Customer> byFirst = customerRepository.searchAny("beta", PageRequest.of(0, 10));
        assertThat(byFirst.getContent()).hasSize(1);
        assertThat(byFirst.getContent().get(0).getFirstName()).isEqualTo("Beta");

        // Match by lastName
        Page<Customer> byLast = customerRepository.searchAny("delta", PageRequest.of(0, 10));
        assertThat(byLast.getContent()).hasSize(1);
        assertThat(byLast.getContent().get(0).getLastName()).isEqualTo("Delta");

        // Match by city
        Page<Customer> byCity = customerRepository.searchAny("ytown", PageRequest.of(0, 10));
        assertThat(byCity.getContent()).hasSize(1);
        assertThat(byCity.getContent().get(0).getCity()).isEqualTo("YTown");

        // No match
        Page<Customer> noMatch = customerRepository.searchAny("notfound", PageRequest.of(0, 10));
        assertThat(noMatch.getContent()).isEmpty();
    }
}

