package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.ReservationStatus;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import com.nextstep.rentacar.exception.SearchValidationException;
import com.nextstep.rentacar.mapper.ReservationMapper;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.CustomerRepository;
import com.nextstep.rentacar.repository.ReservationRepository;
import com.nextstep.rentacar.service.impl.ReservationServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @Spy
    private SimpleMeterRegistry meterRegistry;

    @Mock
    private Page<Reservation> mockReservationPage;

    @Mock
    private Page<ReservationResponseDto> mockResponsePage;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("Search Parameter Tests")
    class SearchParameterTests {

        @Test
        @DisplayName("Should pass search term to unified query when valid search term is provided")
        void shouldUseSearchQueryWhenValidSearchTermProvided() {
            // Given
            String searchTerm = "john doe";
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, null, null, searchTerm, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(searchTerm), eq(pageable));
        }

        @Test
        @DisplayName("Should pass null search to unified query when search term is null")
        void shouldUseFilterQueryWhenSearchTermIsNull() {
            // Given
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, null, null, null, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
        }

        @Test
        @DisplayName("Should pass null search to unified query when search term is blank")
        void shouldUseFilterQueryWhenSearchTermIsEmpty() {
            // Given
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, null, null, "   ", pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
        }

        @Test
        @DisplayName("Should delegate to overloaded method when search parameter is not provided")
        void shouldDelegateToOverloadedMethodWhenSearchParameterNotProvided() {
            // Given
            when(reservationRepository.findWithFiltersAndSearch(
                    eq(1L), eq(2L), eq(ReservationStatus.PENDING), eq(3L), any(), any(), isNull(), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    1L, 2L, ReservationStatus.PENDING, 3L,
                    LocalDate.now(), LocalDate.now().plusDays(1), pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    eq(1L), eq(2L), eq(ReservationStatus.PENDING), eq(3L), any(), any(), isNull(), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Search Validation Tests")
    class SearchValidationTests {

        @Test
        @DisplayName("Should throw exception when search term is too short")
        void shouldThrowExceptionWhenSearchTermTooShort() {
            // Given
            String shortSearch = "a";

            // When & Then
            assertThatThrownBy(() -> reservationService.listWithFilters(
                    null, null, null, null, null, null, shortSearch, pageable))
                    .isInstanceOf(SearchValidationException.class)
                    .hasMessage("Search term must be at least 2 characters long");

            verify(reservationRepository, never()).findWithFiltersAndSearch(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when search term is too long")
        void shouldThrowExceptionWhenSearchTermTooLong() {
            // Given
            String longSearch = "a".repeat(101);

            // When & Then
            assertThatThrownBy(() -> reservationService.listWithFilters(
                    null, null, null, null, null, null, longSearch, pageable))
                    .isInstanceOf(SearchValidationException.class)
                    .hasMessage("Search term cannot exceed 100 characters");

            verify(reservationRepository, never()).findWithFiltersAndSearch(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when search term contains only invalid characters")
        void shouldThrowExceptionWhenSearchTermContainsOnlyInvalidCharacters() {
            // Given
            String invalidSearch = "!#$%^&*<>{}[]|\\";

            // When & Then
            assertThatThrownBy(() -> reservationService.listWithFilters(
                    null, null, null, null, null, null, invalidSearch, pageable))
                    .isInstanceOf(SearchValidationException.class)
                    .hasMessage("Search term contains only invalid characters");

            verify(reservationRepository, never()).findWithFiltersAndSearch(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should sanitize search term and remove invalid characters")
        void shouldSanitizeSearchTermAndRemoveInvalidCharacters() {
            // Given
            String searchWithInvalidChars = "john<script>alert('xss')</script>doe";
            String expectedSanitized = "johnscriptalert(xss)scriptdoe";
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(expectedSanitized), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, null, null, searchWithInvalidChars, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(expectedSanitized), eq(pageable));
        }

        @Test
        @DisplayName("Should accept valid search terms with allowed characters")
        void shouldAcceptValidSearchTermsWithAllowedCharacters() {
            // Given
            String validSearch = "john.doe@email.com (123) 456-7890";
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(validSearch), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, null, null, validSearch, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(validSearch), eq(pageable));
        }

        @Test
        @DisplayName("Should trim whitespace from search term")
        void shouldTrimWhitespaceFromSearchTerm() {
            // Given
            String searchWithWhitespace = "  john doe  ";
            String expectedTrimmed = "john doe";
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(expectedTrimmed), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, null, null, searchWithWhitespace, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(expectedTrimmed), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Date Range Validation Tests")
    class DateRangeValidationTests {

        @Test
        @DisplayName("Should throw exception when end date is before start date")
        void shouldThrowExceptionWhenEndDateBeforeStartDate() {
            // Given
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = LocalDate.now();

            // When & Then
            assertThatThrownBy(() -> reservationService.listWithFilters(
                    null, null, null, null, startDate, endDate, null, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid date range: endDate must be on/after startDate");

            verify(reservationRepository, never()).findWithFiltersAndSearch(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should accept valid date range")
        void shouldAcceptValidDateRange() {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(1);
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), eq(startDate), eq(endDate), isNull(), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, startDate, endDate, null, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), eq(startDate), eq(endDate), isNull(), eq(pageable));
        }

        @Test
        @DisplayName("Should accept same start and end date")
        void shouldAcceptSameStartAndEndDate() {
            // Given
            LocalDate sameDate = LocalDate.now();
            when(reservationRepository.findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), eq(sameDate), eq(sameDate), isNull(), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    null, null, null, null, sameDate, sameDate, null, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    isNull(), isNull(), isNull(), isNull(), eq(sameDate), eq(sameDate), isNull(), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Integration with Existing Filters Tests")
    class IntegrationWithExistingFiltersTests {

        @Test
        @DisplayName("Should pass both search term and filters to the unified query")
        void shouldPassBothSearchAndFiltersToUnifiedQuery() {
            // Given
            String searchTerm = "john";
            Long customerId = 1L;
            ReservationStatus status = ReservationStatus.PENDING;
            when(reservationRepository.findWithFiltersAndSearch(
                    eq(customerId), isNull(), eq(status), isNull(), isNull(), isNull(), eq(searchTerm), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    customerId, null, status, null, null, null, searchTerm, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    eq(customerId), isNull(), eq(status), isNull(), isNull(), isNull(), eq(searchTerm), eq(pageable));
        }

        @Test
        @DisplayName("Should use existing filter logic when search is not provided")
        void shouldUseExistingFilterLogicWhenSearchNotProvided() {
            // Given
            Long customerId = 1L;
            Long carId = 2L;
            ReservationStatus status = ReservationStatus.CONFIRMED;
            Long branchId = 3L;
            when(reservationRepository.findWithFiltersAndSearch(
                    eq(customerId), eq(carId), eq(status), eq(branchId), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(mockReservationPage);
            when(mockReservationPage.map(any(Function.class))).thenReturn(mockResponsePage);

            // When
            Page<ReservationResponseDto> result = reservationService.listWithFilters(
                    customerId, carId, status, branchId, null, null, null, pageable);

            // Then
            assertThat(result).isEqualTo(mockResponsePage);
            verify(reservationRepository).findWithFiltersAndSearch(
                    eq(customerId), eq(carId), eq(status), eq(branchId), isNull(), isNull(), isNull(), eq(pageable));
        }
    }
}
