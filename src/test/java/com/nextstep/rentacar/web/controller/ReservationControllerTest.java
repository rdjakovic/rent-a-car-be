package com.nextstep.rentacar.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.rentacar.config.security.JwtAuthenticationFilter;
import com.nextstep.rentacar.domain.enums.ReservationStatus;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CustomerResponseDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import com.nextstep.rentacar.exception.SearchValidationException;
import com.nextstep.rentacar.service.ReservationService;
import com.nextstep.rentacar.service.auth.CustomUserDetailsService;
import com.nextstep.rentacar.service.auth.JwtService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for ReservationController focusing on search parameter functionality.
 * Tests only the web layer with mocked services.
 */
@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    private ReservationResponseDto sampleReservation;
    private Page<ReservationResponseDto> samplePage;

    @BeforeEach
    void setUp() {
        sampleReservation = createSampleReservation();
        samplePage = new PageImpl<>(
            Collections.singletonList(sampleReservation),
            PageRequest.of(0, 10),
            1
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search parameter")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withSearchParameter_shouldCallServiceWithSearch() throws Exception {
        // Given
        String searchTerm = "John Doe";
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willReturn(samplePage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search with other filters")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withSearchAndOtherFilters_shouldPassAllParameters() throws Exception {
        // Given
        String searchTerm = "john@example.com";
        Long customerId = 1L;
        ReservationStatus status = ReservationStatus.CONFIRMED;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        given(reservationService.listWithFilters(
            eq(customerId), isNull(), eq(status), isNull(), eq(startDate), isNull(), eq(searchTerm), any(Pageable.class)
        )).willReturn(samplePage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("customerId", "1")
                .param("status", "CONFIRMED")
                .param("startDate", "2024-01-01")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(reservationService, times(1)).listWithFilters(
            eq(customerId), isNull(), eq(status), isNull(), eq(startDate), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should work without search parameter (backward compatibility)")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withoutSearchParameter_shouldCallServiceWithoutSearch() throws Exception {
        // Given
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).willReturn(samplePage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle empty search parameter")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withEmptySearchParameter_shouldPassEmptyString() throws Exception {
        // Given
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(""), any(Pageable.class)
        )).willReturn(samplePage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", "")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(""), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search parameter with special characters")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withSpecialCharactersInSearch_shouldPassToService() throws Exception {
        // Given
        String searchTerm = "john.doe@example.com";
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willReturn(samplePage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search parameter with numeric values")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withNumericSearch_shouldPassToService() throws Exception {
        // Given
        String searchTerm = "12345";
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willReturn(samplePage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search validation errors for too short search term")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withTooShortSearch_shouldReturn400WithDetails() throws Exception {
        // Given
        String searchTerm = "x"; // Too short
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willThrow(new SearchValidationException(searchTerm, "Search term must be at least 2 characters long"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Search Validation Error"))
                .andExpect(jsonPath("$.detail").value("Search term must be at least 2 characters long"))
                .andExpect(jsonPath("$.searchTerm").value("x"))
                .andExpect(jsonPath("$.validationError").value("Search term must be at least 2 characters long"))
                .andExpect(jsonPath("$.searchRequirements.minLength").value(2))
                .andExpect(jsonPath("$.searchRequirements.maxLength").value(100));

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search validation errors for too long search term")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withTooLongSearch_shouldReturn400WithDetails() throws Exception {
        // Given
        String searchTerm = "a".repeat(101); // Too long
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willThrow(new SearchValidationException(searchTerm, "Search term cannot exceed 100 characters"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Search Validation Error"))
                .andExpect(jsonPath("$.detail").value("Search term cannot exceed 100 characters"))
                .andExpect(jsonPath("$.searchRequirements.maxLength").value(100));

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle search validation errors for invalid characters")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withInvalidCharacters_shouldReturn400WithDetails() throws Exception {
        // Given
        String searchTerm = "!@#$%^&*"; // Invalid characters only
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willThrow(new SearchValidationException(searchTerm, "Search term contains only invalid characters"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Search Validation Error"))
                .andExpect(jsonPath("$.detail").value("Search term contains only invalid characters"))
                .andExpect(jsonPath("$.searchRequirements.allowedCharacters").exists());

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should handle pagination with search")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withSearchAndPagination_shouldReturnPaginatedResults() throws Exception {
        // Given
        String searchTerm = "Toyota";
        ReservationResponseDto reservation1 = createSampleReservation();
        reservation1.setId(1L);
        ReservationResponseDto reservation2 = createSampleReservation();
        reservation2.setId(2L);
        
        Page<ReservationResponseDto> paginatedResults = new PageImpl<>(
            Arrays.asList(reservation1, reservation2),
            PageRequest.of(0, 2),
            5 // Total elements
        );

        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willReturn(paginatedResults);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "2")
                .param("sort", "id,desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/reservations - should return empty results for search with no matches")
    @WithMockUser(roles = "EMPLOYEE")
    void listReservations_withSearchNoMatches_shouldReturnEmptyPage() throws Exception {
        // Given
        String searchTerm = "nonexistent";
        Page<ReservationResponseDto> emptyPage = new PageImpl<>(
            Collections.emptyList(),
            PageRequest.of(0, 10),
            0
        );

        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        )).willReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", searchTerm)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(reservationService, times(1)).listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), any(Pageable.class)
        );
    }

    private ReservationResponseDto createSampleReservation() {
        ReservationResponseDto reservation = new ReservationResponseDto();
        reservation.setId(1L);
        reservation.setStartDate(LocalDate.of(2024, 6, 1));
        reservation.setEndDate(LocalDate.of(2024, 6, 5));
        reservation.setTotalPrice(BigDecimal.valueOf(400.00));
        reservation.setCurrency("USD");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(OffsetDateTime.of(2024, 5, 15, 10, 0, 0, 0, java.time.ZoneOffset.UTC));
        reservation.setUpdatedAt(OffsetDateTime.of(2024, 5, 15, 10, 0, 0, 0, java.time.ZoneOffset.UTC));
        
        // Set customer info
        CustomerResponseDto customer = new CustomerResponseDto();
        customer.setId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        reservation.setCustomer(customer);
        
        // Set car info
        CarListResponseDto car = new CarListResponseDto();
        car.setId(1L);
        car.setDisplayName("Toyota Camry 2023");
        car.setModel("Camry");
        reservation.setCar(car);
        
        // Set branch info
        BranchResponseDto pickupBranch = new BranchResponseDto();
        pickupBranch.setId(1L);
        pickupBranch.setName("Downtown Branch");
        reservation.setPickupBranch(pickupBranch);
        
        BranchResponseDto dropoffBranch = new BranchResponseDto();
        dropoffBranch.setId(1L);
        dropoffBranch.setName("Downtown Branch");
        reservation.setDropoffBranch(dropoffBranch);
        
        return reservation;
    }
}