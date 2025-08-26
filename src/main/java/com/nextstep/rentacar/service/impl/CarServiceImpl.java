package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.enums.CarCategory;
import com.nextstep.rentacar.domain.enums.FuelType;
import com.nextstep.rentacar.domain.enums.TransmissionType;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import com.nextstep.rentacar.mapper.CarMapper;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.service.CarService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final BranchRepository branchRepository;
    private final CarMapper carMapper;

    @Override
    public CarResponseDto create(CarRequestDto request) {
        if (carRepository.existsByVin(request.getVin())) {
            throw new IllegalArgumentException("Car with VIN already exists: " + request.getVin());
        }
        Car car = carMapper.toEntity(request);
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + request.getBranchId()));
        car.setBranch(branch);
        Car saved = carRepository.save(car);
        return carMapper.toResponseDto(saved);
    }

    @Override
    public CarResponseDto update(Long id, CarRequestDto request) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + id));
        if (!car.getVin().equals(request.getVin()) && carRepository.existsByVin(request.getVin())) {
            throw new IllegalArgumentException("Another car with VIN already exists: " + request.getVin());
        }
        carMapper.updateEntityFromDto(request, car);
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + request.getBranchId()));
            car.setBranch(branch);
        }
        Car saved = carRepository.save(car);
        return carMapper.toResponseDto(saved);
    }

    @Override
    public void delete(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + id));
        car.softDelete();
        carRepository.save(car);
    }

    @Override
    public void restore(Long id) {
        Car car = carRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + id));
        car.restore();
        carRepository.save(car);
    }

    @Override
    @Transactional(readOnly = true)
    public CarResponseDto getById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + id));
        return carMapper.toResponseDto(car);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarListResponseDto> list(Pageable pageable) {
        return carRepository.findAll(pageable).map(carMapper::toListResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarListResponseDto> listByBranch(Long branchId, Pageable pageable) {
        return carRepository.findByBranchId(branchId, pageable).map(carMapper::toListResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarListResponseDto> listDeleted(Pageable pageable) {
        return carRepository.findDeletedCars(pageable).map(carMapper::toListResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarListResponseDto> findAvailable(Long branchId,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  CarCategory category,
                                                  TransmissionType transmission,
                                                  FuelType fuelType,
                                                  Integer minSeats,
                                                  BigDecimal maxPrice,
                                                  Pageable pageable) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Invalid date range: endDate must be after startDate");
        }
        return carRepository.findAvailableCarsWithFilters(branchId, startDate, endDate,
                category, transmission, fuelType, minSeats, maxPrice, pageable)
                .map(carMapper::toListResponseDto);
    }
}
