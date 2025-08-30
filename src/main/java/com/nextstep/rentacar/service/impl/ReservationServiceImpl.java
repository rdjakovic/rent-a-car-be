package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.Customer;
import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.ReservationStatus;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import com.nextstep.rentacar.exception.SearchValidationException;
import com.nextstep.rentacar.mapper.ReservationMapper;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.CustomerRepository;
import com.nextstep.rentacar.repository.ReservationRepository;
import com.nextstep.rentacar.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final BranchRepository branchRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public ReservationResponseDto create(ReservationRequestDto request) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + request.getCarId()));
        Branch pickup = branchRepository.findById(request.getPickupBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Pickup branch not found: " + request.getPickupBranchId()));
        Branch dropoff = branchRepository.findById(request.getDropoffBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Dropoff branch not found: " + request.getDropoffBranchId()));

        ensureCarAvailable(car.getId(), request.getStartDate(), request.getEndDate(), null);

        Reservation reservation = reservationMapper.toEntity(request);
        reservation.setCustomer(customer);
        reservation.setCar(car);
        reservation.setPickupBranch(pickup);
        reservation.setDropoffBranch(dropoff);
        reservation.setStatus(ReservationStatus.PENDING);

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new IllegalArgumentException("Reservation must be at least 1 day");
        }
        BigDecimal totalPrice = car.getDailyPrice().multiply(BigDecimal.valueOf(days));
        reservation.setTotalPrice(totalPrice);
        reservation.setCurrency("USD");

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toResponseDto(saved);
    }

    @Override
    public ReservationResponseDto update(Long id, ReservationRequestDto request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be updated");
        }

        // Map mutable fields
        reservationMapper.updateEntityFromDto(request, reservation);

        // Re-attach associations if changed
        if (!reservation.getCustomer().getId().equals(request.getCustomerId())) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
            reservation.setCustomer(customer);
        }
        if (!reservation.getCar().getId().equals(request.getCarId())) {
            Car car = carRepository.findById(request.getCarId())
                    .orElseThrow(() -> new EntityNotFoundException("Car not found: " + request.getCarId()));
            reservation.setCar(car);
        }
        if (!reservation.getPickupBranch().getId().equals(request.getPickupBranchId())) {
            Branch pickup = branchRepository.findById(request.getPickupBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Pickup branch not found: " + request.getPickupBranchId()));
            reservation.setPickupBranch(pickup);
        }
        if (!reservation.getDropoffBranch().getId().equals(request.getDropoffBranchId())) {
            Branch dropoff = branchRepository.findById(request.getDropoffBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Dropoff branch not found: " + request.getDropoffBranchId()));
            reservation.setDropoffBranch(dropoff);
        }

        // Ensure no overlaps (excluding this reservation)
        ensureCarAvailable(reservation.getCar().getId(), request.getStartDate(), request.getEndDate(), reservation.getId());

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new IllegalArgumentException("Reservation must be at least 1 day");
        }
        BigDecimal totalPrice = reservation.getCar().getDailyPrice().multiply(BigDecimal.valueOf(days));
        reservation.setTotalPrice(totalPrice);

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDto getById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
        return reservationMapper.toResponseDto(reservation);
    }

    @Override
    public void cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
        reservation.cancel();
        reservationRepository.save(reservation);
    }

    @Override
    public void confirm(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
        reservation.confirm();
        reservationRepository.save(reservation);
    }

    @Override
    public void complete(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
        reservation.complete();
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> listByCustomer(Long customerId, Pageable pageable) {
        return reservationRepository.findByCustomerId(customerId, pageable).map(reservationMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> listByCar(Long carId, Pageable pageable) {
        return reservationRepository.findByCarId(carId, pageable).map(reservationMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> listWithFilters(Long customerId,
                                                        Long carId,
                                                        ReservationStatus status,
                                                        Long branchId,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Pageable pageable) {
        return listWithFilters(customerId, carId, status, branchId, startDate, endDate, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> listWithFilters(Long customerId,
                                                        Long carId,
                                                        ReservationStatus status,
                                                        Long branchId,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        String search,
                                                        Pageable pageable) {
        // Validate date range
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid date range: endDate must be on/after startDate");
        }

        // Validate and sanitize search parameter
        String sanitizedSearch = validateAndSanitizeSearch(search);

        // If search is provided, use search-optimized query
        if (sanitizedSearch != null && !sanitizedSearch.trim().isEmpty()) {
            return reservationRepository.findBySearchTerm(sanitizedSearch, pageable)
                    .map(reservationMapper::toResponseDto);
        }

        // Otherwise, use existing filter logic
        if (startDate == null) startDate = LocalDate.now().minusYears(50); // broad default
        if (endDate == null) endDate = LocalDate.now().plusYears(50);
        return reservationRepository.findReservationsInDateRange(customerId, carId, status, branchId, startDate, endDate, pageable)
                .map(reservationMapper::toResponseDto);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    private void ensureCarAvailable(Long carId, LocalDate start, LocalDate end, Long excludeReservationId) {
        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(carId, start, end);
        boolean conflict = overlaps.stream().anyMatch(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId));
        if (conflict) {
            throw new IllegalStateException("Car is not available for the selected dates");
        }
    }

    /**
     * Validates and sanitizes search parameter.
     * @param search the raw search term
     * @return sanitized search term or null if invalid
     * @throws SearchValidationException if search term validation fails
     */
    private String validateAndSanitizeSearch(String search) {
        if (search == null || search.trim().isEmpty()) {
            return null;
        }

        String trimmed = search.trim();
        
        // Minimum length validation (requirement 2.4)
        if (trimmed.length() < 2) {
            throw new SearchValidationException(trimmed, "Search term must be at least 2 characters long");
        }

        // Maximum length validation to prevent abuse
        if (trimmed.length() > 100) {
            throw new SearchValidationException(trimmed, "Search term cannot exceed 100 characters");
        }

        // Sanitize input - remove potentially dangerous characters
        // Allow alphanumeric, spaces, hyphens, dots, @, and common punctuation
        String sanitized = trimmed.replaceAll("[^a-zA-Z0-9\\s\\-\\.@_+()]", "");
        
        if (sanitized.trim().isEmpty()) {
            throw new SearchValidationException(trimmed, "Search term contains only invalid characters");
        }

        return sanitized;
    }
}
