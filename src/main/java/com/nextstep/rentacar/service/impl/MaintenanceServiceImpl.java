package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.Maintenance;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.dto.request.MaintenanceCompleteRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.dto.request.MaintenanceUpdateRequestDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.dto.response.MaintenanceResponseDto;
import com.nextstep.rentacar.mapper.CarMapper;
import com.nextstep.rentacar.mapper.MaintenanceMapper;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.MaintenanceRepository;
import com.nextstep.rentacar.repository.ReservationRepository;
import com.nextstep.rentacar.service.MaintenanceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final CarMapper carMapper;

    @Override
    public MaintenanceResponseDto schedule(MaintenanceScheduleRequestDto request) {
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + request.getCarId()));
        ensureNoReservationOn(car.getId(), request.getScheduledDate());

        Maintenance maintenance = new Maintenance();
        maintenance.setCar(car);
        maintenance.setMaintenanceType(request.getType());
        maintenance.setDescription(request.getDescription());
        maintenance.setScheduledDate(request.getScheduledDate());
        maintenance.setCost(request.getCost());
        maintenance.setNotes(request.getNotes());
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        return maintenanceMapper.toResponseDto(maintenanceRepository.save(maintenance));
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponseDto getById(Long maintenanceId) {
        return maintenanceMapper.toResponseDto(findMaintenance(maintenanceId));
    }

    @Override
    public MaintenanceResponseDto update(Long maintenanceId, MaintenanceUpdateRequestDto request) {
        Maintenance maintenance = findMaintenance(maintenanceId);
        if (maintenance.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new IllegalStateException("Only SCHEDULED maintenance can be edited, current status: " + maintenance.getStatus());
        }
        if (!request.getScheduledDate().equals(maintenance.getScheduledDate())) {
            if (request.getScheduledDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Scheduled date cannot be in the past");
            }
            ensureNoReservationOn(maintenance.getCar().getId(), request.getScheduledDate());
        }
        maintenance.setMaintenanceType(request.getType());
        maintenance.setDescription(request.getDescription());
        maintenance.setScheduledDate(request.getScheduledDate());
        maintenance.setCost(request.getCost());
        maintenance.setNotes(request.getNotes());
        return maintenanceMapper.toResponseDto(maintenanceRepository.save(maintenance));
    }

    @Override
    public MaintenanceResponseDto start(Long maintenanceId) {
        Maintenance maintenance = findMaintenance(maintenanceId);
        Car car = maintenance.getCar();
        if (car.getStatus() == CarStatus.RENTED) {
            throw new IllegalStateException("Car is out on rental; start the job after it has been returned");
        }
        maintenance.start();
        // Set car status to MAINTENANCE
        car.setStatus(CarStatus.MAINTENANCE);
        carRepository.save(car);
        return maintenanceMapper.toResponseDto(maintenanceRepository.save(maintenance));
    }

    @Override
    public MaintenanceResponseDto complete(Long maintenanceId, MaintenanceCompleteRequestDto details) {
        Maintenance maintenance = findMaintenance(maintenanceId);
        maintenance.complete();
        Car car = maintenance.getCar();

        if (details != null) {
            if (details.getCost() != null) {
                maintenance.setCost(details.getCost());
            }
            if (details.getNotes() != null) {
                maintenance.setNotes(details.getNotes());
            }
            if (details.getOdometerReading() != null) {
                if (details.getOdometerReading() < car.getMileage()) {
                    throw new IllegalArgumentException("Odometer reading " + details.getOdometerReading()
                            + " is lower than the car's recorded mileage " + car.getMileage());
                }
                maintenance.setOdometerReading(details.getOdometerReading());
                car.setMileage(details.getOdometerReading());
            }
            if (details.getNextServiceDate() != null) {
                car.setNextServiceDate(details.getNextServiceDate());
            }
        }
        car.setLastServiceDate(maintenance.getCompletedDate());
        releaseCarIfIdle(car, maintenance);
        carRepository.save(car);
        return maintenanceMapper.toResponseDto(maintenanceRepository.save(maintenance));
    }

    @Override
    public MaintenanceResponseDto cancel(Long maintenanceId) {
        Maintenance maintenance = findMaintenance(maintenanceId);
        boolean wasInProgress = maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS;
        maintenance.cancel();
        // Only a job in progress took the car off the road; a scheduled one never changed its status
        if (wasInProgress) {
            Car car = maintenance.getCar();
            releaseCarIfIdle(car, maintenance);
            carRepository.save(car);
        }
        return maintenanceMapper.toResponseDto(maintenanceRepository.save(maintenance));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponseDto> listByFilters(Long carId, Long employeeId, MaintenanceStatus status, MaintenanceType maintenanceType, Long branchId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return maintenanceRepository.findWithFilters(carId, employeeId, status, maintenanceType, branchId, startDate, endDate, pageable)
                .map(maintenanceMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponseDto> findScheduledForDate(LocalDate date) {
        return maintenanceRepository.findScheduledForDate(date).stream()
                .map(maintenanceMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarResponseDto> findCarsDueForService(int withinDays) {
        return carRepository.findCarsDueForServiceWithoutOpenMaintenance(LocalDate.now().plusDays(withinDays)).stream()
                .map(carMapper::toResponseDto)
                .toList();
    }

    private Maintenance findMaintenance(Long maintenanceId) {
        return maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new EntityNotFoundException("Maintenance not found: " + maintenanceId));
    }

    /** A car can't go to the shop on a day it is booked. */
    private void ensureNoReservationOn(Long carId, LocalDate date) {
        if (!reservationRepository.findOverlappingReservations(carId, date, date).isEmpty()) {
            throw new IllegalStateException("Car has a reservation on " + date + "; pick another date");
        }
    }

    /** Back to AVAILABLE once no other job keeps the car in the shop (soft-deleted cars stay as they are). */
    private void releaseCarIfIdle(Car car, Maintenance finished) {
        boolean otherJobInProgress = maintenanceRepository.findByCarIdAndStatus(car.getId(), MaintenanceStatus.IN_PROGRESS)
                .stream().anyMatch(m -> !m.getId().equals(finished.getId()));
        if (Boolean.FALSE.equals(car.getDeleted()) && !otherJobInProgress) {
            car.setStatus(CarStatus.AVAILABLE);
        }
    }
}
