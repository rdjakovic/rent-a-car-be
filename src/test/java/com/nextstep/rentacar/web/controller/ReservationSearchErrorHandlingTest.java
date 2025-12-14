package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.exception.SearchValidationException;
import com.nextstep.rentacar.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for search error handling in ReservationController.
 * Tests the complete error handling flow from controller to global exception handler.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationSearchErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @Test
    @DisplayName("Search validation error should return proper error response with search requirements")
    @WithMockUser(roles = "EMPLOYEE")
    void searchValidationError_shouldReturnProperErrorResponse() throws Exception {
        // Given
        String shortSearchTerm = "a";
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(shortSearchTerm), any(Pageable.class)
        )).willThrow(new SearchValidationException(shortSearchTerm, "Search term must be at least 2 characters long"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", shortSearchTerm))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Search Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Search term must be at least 2 characters long"))
                .andExpect(jsonPath("$.path").value("/api/reservations"))
                .andExpect(jsonPath("$.searchTerm").value("a"))
                .andExpect(jsonPath("$.validationError").value("Search term must be at least 2 characters long"))
                .andExpect(jsonPath("$.searchRequirements").exists())
                .andExpect(jsonPath("$.searchRequirements.minLength").value(2))
                .andExpect(jsonPath("$.searchRequirements.maxLength").value(100))
                .andExpect(jsonPath("$.searchRequirements.allowedCharacters").exists());
    }

    @Test
    @DisplayName("Search validation error for too long term should include proper details")
    @WithMockUser(roles = "EMPLOYEE")
    void searchValidationErrorTooLong_shouldReturnProperErrorResponse() throws Exception {
        // Given
        String longSearchTerm = "a".repeat(101);
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(longSearchTerm), any(Pageable.class)
        )).willThrow(new SearchValidationException(longSearchTerm, "Search term cannot exceed 100 characters"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", longSearchTerm))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Search Validation Error"))
                .andExpect(jsonPath("$.detail").value("Search term cannot exceed 100 characters"))
                .andExpect(jsonPath("$.searchRequirements.maxLength").value(100));
    }

    @Test
    @DisplayName("Search validation error for invalid characters should include character requirements")
    @WithMockUser(roles = "EMPLOYEE")
    void searchValidationErrorInvalidChars_shouldReturnProperErrorResponse() throws Exception {
        // Given
        String invalidSearchTerm = "!@#$%^&*";
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(invalidSearchTerm), any(Pageable.class)
        )).willThrow(new SearchValidationException(invalidSearchTerm, "Search term contains only invalid characters"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", invalidSearchTerm))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Search Validation Error"))
                .andExpect(jsonPath("$.detail").value("Search term contains only invalid characters"))
                .andExpect(jsonPath("$.searchTerm").value("!@#$%^&*"))
                .andExpect(jsonPath("$.searchRequirements.allowedCharacters").value("alphanumeric, spaces, hyphens, dots, @ symbols, underscores, plus signs, parentheses"));
    }

    @Test
    @DisplayName("Regular IllegalArgumentException should still work for backward compatibility")
    @WithMockUser(roles = "EMPLOYEE")
    void regularIllegalArgumentException_shouldReturnBadRequest() throws Exception {
        // Given
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("test"), any(Pageable.class)
        )).willThrow(new IllegalArgumentException("Invalid date range: endDate must be on/after startDate"));

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", "test"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid date range: endDate must be on/after startDate"))
                .andExpect(jsonPath("$.searchTerm").doesNotExist()) // Should not have search-specific properties
                .andExpect(jsonPath("$.searchRequirements").doesNotExist());
    }

    @Test
    @DisplayName("Valid search should work normally")
    @WithMockUser(roles = "EMPLOYEE")
    void validSearch_shouldReturnResults() throws Exception {
        // Given
        String validSearchTerm = "John Doe";
        Page<Object> emptyPage = new PageImpl<>(Collections.emptyList());
        given(reservationService.listWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(validSearchTerm), any(Pageable.class)
        )).willReturn((Page) emptyPage);

        // When & Then
        mockMvc.perform(get("/api/reservations")
                .param("search", validSearchTerm))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}