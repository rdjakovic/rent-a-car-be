package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.Maintenance;
import com.nextstep.rentacar.domain.enums.CarStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceStatus;
import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.repository.MaintenanceRepository;
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

    @Override
    public Maintenance schedule(Long carId, MaintenanceType type, String description, LocalDate scheduledDate) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));
        Maintenance maintenance = new Maintenance();
        maintenance.setCar(car);
        maintenance.setMaintenanceType(type);
        maintenance.setDescription(description);
        maintenance.setScheduledDate(scheduledDate);
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        return maintenanceRepository.save(maintenance);
    }

    @Override
    public Maintenance start(Long maintenanceId) {
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new EntityNotFoundException("Maintenance not found: " + maintenanceId));
        maintenance.start();
        // Set car status to MAINTENANCE
        Car car = maintenance.getCar();
        car.setStatus(CarStatus.MAINTENANCE);
        carRepository.save(car);
        return maintenanceRepository.save(maintenance);
    }

    @Override
    public Maintenance complete(Long maintenanceId) {
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new EntityNotFoundException("Maintenance not found: " + maintenanceId));
        maintenance.complete();
        // Restore car status to AVAILABLE if not soft-deleted
        Car car = maintenance.getCar();
        if (Boolean.FALSE.equals(car.getDeleted())) {
            car.setStatus(CarStatus.AVAILABLE);
            carRepository.save(car);
        }
        return maintenanceRepository.save(maintenance);
    }

    @Override
    public Maintenance cancel(Long maintenanceId) {
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new EntityNotFoundException("Maintenance not found: " + maintenanceId));
        maintenance.cancel();
        // If cancelling from IN_PROGRESS, restore car status
        Car car = maintenance.getCar();
        if (maintenance.getStatus() == MaintenanceStatus.CANCELLED && Boolean.FALSE.equals(car.getDeleted())) {
            car.setStatus(CarStatus.AVAILABLE);
            carRepository.save(car);
        }
        return maintenanceRepository.save(maintenance);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Maintenance> listByFilters(Long carId, Long employeeId, MaintenanceStatus status, MaintenanceType maintenanceType, Long branchId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return maintenanceRepository.findWithFilters(carId, employeeId, status, maintenanceType, branchId, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Maintenance> findScheduledForDate(LocalDate date) {
        return maintenanceRepository.findScheduledForDate(date);
    }
}
