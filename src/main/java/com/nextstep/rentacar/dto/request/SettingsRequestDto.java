package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.MileageUnit;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsRequestDto {

    @NotNull(message = "Mileage unit is required")
    private MileageUnit mileageUnit;

    /** Must be an active branch; null clears the default. */
    private Long defaultBranchId;
}
