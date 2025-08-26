package com.nextstep.rentacar.service;

import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BranchServiceIntegrationTest {

    @Autowired private BranchService branchService;

    private BranchRequestDto sample(String name) {
        return new BranchRequestDto(
                name,
                "10 Market St",
                "Star City",
                "USA",
                "+12223334444",
                "branch@star.com",
                "Mon-Sun 8-20",
                true
        );
    }

    @Test
    @DisplayName("Branch create, duplicate check, search and delete")
    void branchCrudAndValidations() {
        BranchResponseDto b1 = branchService.create(sample("Downtown"));
        assertThat(b1.getId()).isNotNull();

        // Duplicate name+city
        assertThatThrownBy(() -> branchService.create(sample("Downtown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        // Search by name
        Page<BranchResponseDto> search = branchService.searchByName("Down", PageRequest.of(0, 10));
        assertThat(search.getTotalElements()).isGreaterThanOrEqualTo(1);

        // Find by city
        assertThat(branchService.findByCity("Star City")).extracting(BranchResponseDto::getName).contains("Downtown");

        // Delete
        branchService.delete(b1.getId());
        assertThatThrownBy(() -> branchService.getById(b1.getId()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}
