package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.AppSetting;
import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.enums.MileageUnit;
import com.nextstep.rentacar.dto.request.SettingsRequestDto;
import com.nextstep.rentacar.dto.response.SettingsResponseDto;
import com.nextstep.rentacar.repository.AppSettingRepository;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.service.SettingsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsServiceImpl implements SettingsService {

    static final String MILEAGE_UNIT = "mileage_unit";
    static final String DEFAULT_BRANCH_ID = "default_branch_id";
    static final MileageUnit DEFAULT_MILEAGE_UNIT = MileageUnit.KILOMETERS;

    private final AppSettingRepository appSettingRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional(readOnly = true)
    public SettingsResponseDto get() {
        MileageUnit mileageUnit = appSettingRepository.findById(MILEAGE_UNIT)
                .map(setting -> MileageUnit.valueOf(setting.getValue()))
                .orElse(DEFAULT_MILEAGE_UNIT);
        return new SettingsResponseDto(mileageUnit, defaultBranchId());
    }

    /** The saved default branch, or null when none is set or it has since been closed or removed. */
    private Long defaultBranchId() {
        return appSettingRepository.findById(DEFAULT_BRANCH_ID)
                .map(setting -> Long.valueOf(setting.getValue()))
                .flatMap(branchRepository::findById)
                .filter(Branch::isActive)
                .map(Branch::getId)
                .orElse(null);
    }

    @Override
    public SettingsResponseDto update(SettingsRequestDto request) {
        AppSetting setting = appSettingRepository.findById(MILEAGE_UNIT)
                .orElseGet(() -> new AppSetting(MILEAGE_UNIT, null));
        setting.setValue(request.getMileageUnit().name());
        appSettingRepository.save(setting);
        saveDefaultBranch(request.getDefaultBranchId());
        return get();
    }

    private void saveDefaultBranch(Long branchId) {
        if (branchId == null) {
            appSettingRepository.deleteById(DEFAULT_BRANCH_ID);
            return;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
        if (!branch.isActive()) {
            throw new IllegalStateException("Default branch is inactive: " + branch.getName());
        }
        AppSetting setting = appSettingRepository.findById(DEFAULT_BRANCH_ID)
                .orElseGet(() -> new AppSetting(DEFAULT_BRANCH_ID, null));
        setting.setValue(branchId.toString());
        appSettingRepository.save(setting);
    }
}
