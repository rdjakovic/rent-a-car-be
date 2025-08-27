package com.nextstep.rentacar.service;

import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.response.CustomerResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponseDto create(CustomerRequestDto request);

    CustomerResponseDto update(Long id, CustomerRequestDto request);

    CustomerResponseDto getById(Long id);

    void delete(Long id);

    Page<CustomerResponseDto> list(Pageable pageable);

    Page<CustomerResponseDto> search(String email,
                                     String firstName,
                                     String lastName,
                                     String city,
                                     Pageable pageable);

    Page<CustomerResponseDto> searchAny(String search, Pageable pageable);
}
