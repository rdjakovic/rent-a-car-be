package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.Customer;
import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.domain.enums.ReservationStatus;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.dto.request.ReservationPickupRequestDto;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.request.ReservationReturnRequestDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import com.nextstep.rentacar.exception.SearchValidationException;
import com.nextstep.rentacar.mapper.ReservationMapper;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.CustomerRepository;
import com.nextstep.rentacar.repository.MaintenanceRepository;
import com.nextstep.rentacar.repository.ReservationRepository;
import com.nextstep.rentacar.service.ReservationService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    /** Response-time target for reservation search (requirement 1.5). */
    private static final long SEARCH_DURATION_WARN_MS = 500;

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final BranchRepository branchRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ReservationMapper reservationMapper;
    private final MeterRegistry meterRegistry;

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
        ensureBranchOpen(pickup, "Pickup");
        ensureBranchOpen(dropoff, "Dropoff");

        ensureCarAvailable(car.getId(), request.getStartDate(), request.getEndDate(), null);

        Reservation reservation = reservationMapper.toEntity(request);
        reservation.setCustomer(customer);
        reservation.setCar(car);
        reservation.setPickupBranch(pickup);
        reservation.setDropoffBranch(dropoff);
        reservation.setStatus(ReservationStatus.PENDING);

        long days = Reservation.rentalDays(request.getStartDate(), request.getEndDate());
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
            ensureBranchOpen(pickup, "Pickup");
            reservation.setPickupBranch(pickup);
        }
        if (!reservation.getDropoffBranch().getId().equals(request.getDropoffBranchId())) {
            Branch dropoff = branchRepository.findById(request.getDropoffBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Dropoff branch not found: " + request.getDropoffBranchId()));
            ensureBranchOpen(dropoff, "Dropoff");
            reservation.setDropoffBranch(dropoff);
        }

        // Ensure no overlaps (excluding this reservation)
        ensureCarAvailable(reservation.getCar().getId(), request.getStartDate(), request.getEndDate(), reservation.getId());

        long days = Reservation.rentalDays(request.getStartDate(), request.getEndDate());
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
    public void pickUp(Long id, ReservationPickupRequestDto details) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
        Car car = reservation.getCar();
        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new IllegalStateException("Car cannot be handed over, its status is " + car.getStatus());
        }
        Integer mileageOut = details != null ? details.getMileageOut() : null;
        ensureOdometerNotBelowMileage(car, mileageOut);

        reservation.pickUp(mileageOut);
        if (mileageOut != null) {
            car.setMileage(mileageOut);
        }
        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        reservationRepository.save(reservation);
    }

    @Override
    public void complete(Long id, ReservationReturnRequestDto details) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
        Integer mileageIn = details != null ? details.getMileageIn() : null;
        if (mileageIn != null && reservation.getMileageOut() != null && mileageIn < reservation.getMileageOut()) {
            throw new IllegalArgumentException("Return odometer " + mileageIn
                    + " is lower than the odometer at pick-up " + reservation.getMileageOut());
        }
        Car car = reservation.getCar();
        ensureOdometerNotBelowMileage(car, mileageIn);

        reservation.complete(mileageIn);
        if (mileageIn != null) {
            car.setMileage(mileageIn);
        }
        // The car is back where the customer dropped it off (one-way rentals move it)
        car.setBranch(reservation.getDropoffBranch());
        if (Boolean.FALSE.equals(car.getDeleted())) {
            car.setStatus(CarStatus.AVAILABLE);
        }
        carRepository.save(car);
        reservationRepository.save(reservation);
    }

    /** An odometer reading becomes the car's mileage, so it cannot go backwards. */
    private void ensureOdometerNotBelowMileage(Car car, Integer odometer) {
        if (odometer != null && odometer < car.getMileage()) {
            throw new IllegalArgumentException("Odometer reading " + odometer
                    + " is lower than the car's recorded mileage " + car.getMileage());
        }
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
        boolean searchUsed = sanitizedSearch != null;

        // Use the unified method that handles both search and filters
        long startNanos = System.nanoTime();
        Page<Reservation> result = reservationRepository.findWithFiltersAndSearch(
                customerId, carId, status, branchId, startDate, endDate, sanitizedSearch, pageable);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        meterRegistry.timer("reservation.search.duration", "search", Boolean.toString(searchUsed))
                .record(durationMs, TimeUnit.MILLISECONDS);
        log.debug("Reservation search completed in {} ms (searchUsed={}, totalResults={})",
                durationMs, searchUsed, result.getTotalElements());
        if (durationMs > SEARCH_DURATION_WARN_MS) {
            log.warn("Reservation search exceeded {} ms target: {} ms (searchUsed={}, searchTermLength={}, totalResults={})",
                    SEARCH_DURATION_WARN_MS, durationMs, searchUsed,
                    searchUsed ? sanitizedSearch.length() : 0, result.getTotalElements());
        }

        return result.map(reservationMapper::toResponseDto);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    /** A closed branch can't be chosen; a reservation that keeps its (since closed) branch can still be edited. */
    private void ensureBranchOpen(Branch branch, String role) {
        if (!branch.isActive()) {
            throw new IllegalStateException(role + " branch is inactive: " + branch.getName());
        }
    }

    private void ensureCarAvailable(Long carId, LocalDate start, LocalDate end, Long excludeReservationId) {
        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(carId, start, end);
        boolean conflict = overlaps.stream().anyMatch(r -> excludeReservationId == null || !r.getId().equals(excludeReservationId));
        if (conflict) {
            throw new IllegalStateException("Car is not available for the selected dates");
        }
        if (!maintenanceRepository.findBlockingMaintenance(carId, start, end, LocalDate.now()).isEmpty()) {
            throw new IllegalStateException("Car is in maintenance or scheduled for maintenance during the selected dates");
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
