package com.nextstep.rentacar.service;

import com.nextstep.rentacar.dto.request.SettingsRequestDto;
import com.nextstep.rentacar.dto.response.SettingsResponseDto;

public interface SettingsService {

    SettingsResponseDto get();

    SettingsResponseDto update(SettingsRequestDto request);
}
