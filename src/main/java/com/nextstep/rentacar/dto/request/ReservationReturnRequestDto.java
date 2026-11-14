package com.nextstep.rentacar.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Optional details recorded when the car comes back. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationReturnRequestDto {

    /** Odometer at return, in the fleet's mileage unit; updates the car's mileage */
    @PositiveOrZero
    private Integer mileageIn;
}
