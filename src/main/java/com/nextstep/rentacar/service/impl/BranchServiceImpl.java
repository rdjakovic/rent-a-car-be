package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import com.nextstep.rentacar.mapper.BranchMapper;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.service.BranchService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public BranchResponseDto create(BranchRequestDto request) {
        if (branchRepository.existsByNameAndCity(request.getName(), request.getCity())) {
            throw new IllegalArgumentException("Branch with the same name already exists in this city");
        }
        Branch branch = branchMapper.toEntity(request);
        Branch saved = branchRepository.save(branch);
        return branchMapper.toResponseDto(saved);
    }

    @Override
    public BranchResponseDto update(Long id, BranchRequestDto request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + id));
        if (!branch.getName().equals(request.getName()) && branchRepository.existsByNameAndCity(request.getName(), request.getCity())) {
            throw new IllegalArgumentException("Branch with the same name already exists in this city");
        }
        branchMapper.updateEntityFromDto(request, branch);
        Branch saved = branchRepository.save(branch);
        return branchMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponseDto getById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + id));
        return branchMapper.toResponseDto(branch);
    }

    @Override
    public void delete(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new EntityNotFoundException("Branch not found: " + id);
        }
        branchRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponseDto> list(Pageable pageable) {
        return branchRepository.findAll(pageable).map(branchMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponseDto> searchByName(String name, Pageable pageable) {
        return branchRepository.findByNameContainingIgnoreCase(name, pageable).map(branchMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponseDto> findByCity(String city) {
        return branchRepository.findByCity(city).stream().map(branchMapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponseDto> findByCountry(String country) {
        return branchRepository.findByCountry(country).stream().map(branchMapper::toResponseDto).toList();
    }
}
