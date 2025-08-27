package com.nextstep.rentacar.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.rentacar.config.security.JwtAuthenticationFilter;
import com.nextstep.rentacar.service.auth.JwtService;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import com.nextstep.rentacar.service.BranchService;
import com.nextstep.rentacar.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.nextstep.rentacar.testutil.builders.BranchTestDataBuilder.aBranch;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for BranchController using @WebMvcTest.
 * Tests only the web layer with mocked services.
 * Faster than full integration tests.
 */
@WebMvcTest(BranchController.class)
@AutoConfigureMockMvc(addFilters = false)
class BranchControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BranchService branchService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    private BranchResponseDto sampleBranchResponse;

    @BeforeEach
    void setUp() {
        sampleBranchResponse = new BranchResponseDto();
        sampleBranchResponse.setId(1L);
        sampleBranchResponse.setName("Test Branch");
        sampleBranchResponse.setAddress("123 Main St");
        sampleBranchResponse.setCity("New York");
        sampleBranchResponse.setCountry("USA");
        sampleBranchResponse.setPhone("+1-555-0100");
        sampleBranchResponse.setEmail("branch@test.com");
        sampleBranchResponse.setOpeningHours("9:00-18:00");
        sampleBranchResponse.setActive(true);
    }

    @Test
    @DisplayName("POST /api/branches - should create branch")
    @WithMockUser(roles = "ADMIN")
    void createBranch_shouldReturnCreatedBranch() throws Exception {
        // Given
        BranchRequestDto request = aBranch().inNewYork().build();
        given(branchService.create(any(BranchRequestDto.class))).willReturn(sampleBranchResponse);

        // When & Then
        mockMvc.perform(post("/api/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Branch"))
                .andExpect(jsonPath("$.city").value("New York"));

        verify(branchService, times(1)).create(any(BranchRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/branches - should validate request")
    @WithMockUser(roles = "ADMIN")
    void createBranch_withInvalidData_shouldReturn400() throws Exception {
        // Given - Branch without required fields
        BranchRequestDto request = new BranchRequestDto(
            null, // name is required
            null, // address is required
            null,
            null,
            null,
            null,
            null,
            true
        );

        // When & Then
        mockMvc.perform(post("/api/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(branchService, never()).create(any());
    }

    @Test
    @DisplayName("GET /api/branches - should return paginated list")
    @WithMockUser(roles = "EMPLOYEE")
    void listBranches_shouldReturnPaginatedResults() throws Exception {
        // Given
        BranchResponseDto branch1 = new BranchResponseDto();
        branch1.setId(1L);
        branch1.setName("Branch 1");
        branch1.setCity("New York");

        BranchResponseDto branch2 = new BranchResponseDto();
        branch2.setId(2L);
        branch2.setName("Branch 2");
        branch2.setCity("Los Angeles");

        Page<BranchResponseDto> page = new PageImpl<>(
            Arrays.asList(branch1, branch2),
            PageRequest.of(0, 10),
            2
        );

        given(branchService.list(any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/branches")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Branch 1"))
                .andExpect(jsonPath("$.content[1].name").value("Branch 2"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/branches/{id} - should return branch by id")
    @WithMockUser(roles = "EMPLOYEE")
    void getBranchById_shouldReturnBranch() throws Exception {
        // Given
        given(branchService.getById(1L)).willReturn(sampleBranchResponse);

        // When & Then
        mockMvc.perform(get("/api/branches/{id}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Branch"));

        verify(branchService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("PUT /api/branches/{id} - should update branch")
    @WithMockUser(roles = "ADMIN")
    void updateBranch_shouldReturnUpdatedBranch() throws Exception {
        // Given
        BranchRequestDto request = aBranch().withName("Updated Branch").build();
        BranchResponseDto updatedResponse = new BranchResponseDto();
        updatedResponse.setId(1L);
        updatedResponse.setName("Updated Branch");

        given(branchService.update(eq(1L), any(BranchRequestDto.class))).willReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/branches/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Branch"));

        verify(branchService, times(1)).update(eq(1L), any(BranchRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/branches/{id} - should delete branch")
    @WithMockUser(roles = "ADMIN")
    void deleteBranch_shouldReturn204() throws Exception {
        // Given
        doNothing().when(branchService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/branches/{id}", 1L))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(branchService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("GET /api/branches/search - should search by name")
    @WithMockUser(roles = "EMPLOYEE")
    void searchBranchesByName_shouldReturnFilteredResults() throws Exception {
        // Given
        Page<BranchResponseDto> searchResults = new PageImpl<>(
            Collections.singletonList(sampleBranchResponse),
            PageRequest.of(0, 10),
            1
        );

        given(branchService.searchByName(eq("Test"), any(Pageable.class))).willReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/branches/search")
                .param("name", "Test")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Branch"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(branchService, times(1)).searchByName(eq("Test"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/branches/by-city - should return branches by city")
    @WithMockUser(roles = "EMPLOYEE")
    void getBranchesByCity_shouldReturnFilteredList() throws Exception {
        // Given
        List<BranchResponseDto> cityBranches = Arrays.asList(sampleBranchResponse);
        given(branchService.findByCity("New York")).willReturn(cityBranches);

        // When & Then
        mockMvc.perform(get("/api/branches/by-city")
                .param("city", "New York"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].city").value("New York"));

        verify(branchService, times(1)).findByCity("New York");
    }

    @Test
    @DisplayName("GET /api/branches/by-country - should return branches by country")
    @WithMockUser(roles = "EMPLOYEE")
    void getBranchesByCountry_shouldReturnFilteredList() throws Exception {
        // Given
        List<BranchResponseDto> countryBranches = Arrays.asList(sampleBranchResponse);
        given(branchService.findByCountry("USA")).willReturn(countryBranches);

        // When & Then
        mockMvc.perform(get("/api/branches/by-country")
                .param("country", "USA"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].country").value("USA"));

        verify(branchService, times(1)).findByCountry("USA");
    }

    // Note: Security tests are disabled because we're using @AutoConfigureMockMvc(addFilters = false)
}
