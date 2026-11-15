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

    /** Branch pre-selected on the availability page; null when none is set. */
    private Long defaultBranchId;
}
