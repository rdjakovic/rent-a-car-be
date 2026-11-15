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
        Page<BranchResponseDto> search = branchService.search("Down", null, PageRequest.of(0, 10));
        assertThat(search.getTotalElements()).isGreaterThanOrEqualTo(1);

        // Find by city
        assertThat(branchService.findByCity("Star City")).extracting(BranchResponseDto::getName).contains("Downtown");

        // Delete
        branchService.delete(b1.getId());
        assertThatThrownBy(() -> branchService.getById(b1.getId()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Search matches name and/or city partially, ignoring case; blank filters are ignored")
    void searchByNameAndCity() {
        branchService.create(branchIn("Harbor Gate", "Port Royal"));
        branchService.create(branchIn("Harbor Plaza", "Port Elizabeth"));
        branchService.create(branchIn("Hilltop", "Port Royal"));
        PageRequest page = PageRequest.of(0, 50);

        assertThat(names(branchService.search("harbor", null, page))).containsExactlyInAnyOrder("Harbor Gate", "Harbor Plaza");
        assertThat(names(branchService.search(null, "port royal", page))).containsExactlyInAnyOrder("Harbor Gate", "Hilltop");
        assertThat(names(branchService.search("HARBOR", "royal", page))).containsExactly("Harbor Gate");
        assertThat(names(branchService.search("  ", "", page))).contains("Harbor Gate", "Harbor Plaza", "Hilltop");
        assertThat(branchService.search("nothing like this", null, page).getContent()).isEmpty();
    }

    private static java.util.List<String> names(Page<BranchResponseDto> page) {
        return page.getContent().stream().map(BranchResponseDto::getName).toList();
    }

    private static BranchRequestDto branchIn(String name, String city) {
        return new BranchRequestDto(name, "1 Quay St", city, "USA", "+12223335555", name.replace(' ', '.').toLowerCase() + "@example.com", "Daily 8-20", true);
    }
}
