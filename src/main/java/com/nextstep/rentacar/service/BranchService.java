package com.nextstep.rentacar.service;

import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BranchService {

    BranchResponseDto create(BranchRequestDto request);

    BranchResponseDto update(Long id, BranchRequestDto request);

    BranchResponseDto getById(Long id);

    void delete(Long id);

    Page<BranchResponseDto> list(Pageable pageable);

    Page<BranchResponseDto> searchByName(String name, Pageable pageable);

    List<BranchResponseDto> findByCity(String city);

    List<BranchResponseDto> findByCountry(String country);

    List<BranchResponseDto> findByNameAndCity(String name, String city, Pageable pageable);
}
