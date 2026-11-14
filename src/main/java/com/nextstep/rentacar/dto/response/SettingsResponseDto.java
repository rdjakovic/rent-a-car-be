package com.nextstep.rentacar.dto.response;

import com.nextstep.rentacar.domain.enums.MileageUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsResponseDto {

    private MileageUnit mileageUnit;
}
