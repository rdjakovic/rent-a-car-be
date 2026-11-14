package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.AppSetting;
import com.nextstep.rentacar.domain.enums.MileageUnit;
import com.nextstep.rentacar.dto.request.SettingsRequestDto;
import com.nextstep.rentacar.dto.response.SettingsResponseDto;
import com.nextstep.rentacar.repository.AppSettingRepository;
import com.nextstep.rentacar.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsServiceImpl implements SettingsService {

    static final String MILEAGE_UNIT = "mileage_unit";
    static final MileageUnit DEFAULT_MILEAGE_UNIT = MileageUnit.KILOMETERS;

    private final AppSettingRepository appSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public SettingsResponseDto get() {
        MileageUnit mileageUnit = appSettingRepository.findById(MILEAGE_UNIT)
                .map(setting -> MileageUnit.valueOf(setting.getValue()))
                .orElse(DEFAULT_MILEAGE_UNIT);
        return new SettingsResponseDto(mileageUnit);
    }

    @Override
    public SettingsResponseDto update(SettingsRequestDto request) {
        AppSetting setting = appSettingRepository.findById(MILEAGE_UNIT)
                .orElseGet(() -> new AppSetting(MILEAGE_UNIT, null));
        setting.setValue(request.getMileageUnit().name());
        appSettingRepository.save(setting);
        return get();
    }
}
