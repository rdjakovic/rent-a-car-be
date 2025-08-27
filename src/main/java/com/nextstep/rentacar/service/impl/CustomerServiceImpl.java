package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Customer;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.response.CustomerResponseDto;
import com.nextstep.rentacar.mapper.CustomerMapper;
import com.nextstep.rentacar.repository.CustomerRepository;
import com.nextstep.rentacar.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponseDto create(CustomerRequestDto request) {
        validateCustomer(request);
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }
        if (customerRepository.existsByDriverLicenseNo(request.getDriverLicenseNo())) {
            throw new IllegalArgumentException("Driver license already in use: " + request.getDriverLicenseNo());
        }
        Customer entity = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(entity);
        return customerMapper.toResponseDto(saved);
    }

    @Override
    public CustomerResponseDto update(Long id, CustomerRequestDto request) {
        validateCustomer(request);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));

        if (!customer.getEmail().equals(request.getEmail()) && customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }
        if (!customer.getDriverLicenseNo().equals(request.getDriverLicenseNo()) &&
                customerRepository.existsByDriverLicenseNo(request.getDriverLicenseNo())) {
            throw new IllegalArgumentException("Driver license already in use: " + request.getDriverLicenseNo());
        }

        customerMapper.updateEntityFromDto(request, customer);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        return customerMapper.toResponseDto(customer);
    }

    @Override
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> list(Pageable pageable) {
        return customerRepository.findAll(pageable).map(customerMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> search(String email, String firstName, String lastName, String city, Pageable pageable) {
        return customerRepository.findWithFilters(email, firstName, lastName, city, pageable)
                .map(customerMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> searchAny(String search, Pageable pageable) {
        return customerRepository.searchAny(search, pageable)
                .map(customerMapper::toResponseDto);
    }

    private void validateCustomer(CustomerRequestDto request) {
        LocalDate dob = request.getDateOfBirth();
        if (dob == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
        if (request.getLicenseExpiryDate() != null && !request.getLicenseExpiryDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Driver license expiry date must be in the future");
        }
    }
}
