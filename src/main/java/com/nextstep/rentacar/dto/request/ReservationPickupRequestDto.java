package com.nextstep.rentacar.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Optional details recorded when the customer picks the car up. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPickupRequestDto {

    /** Odometer at hand-over, in the fleet's mileage unit; cannot be below the car's recorded mileage */
    @PositiveOrZero
    private Integer mileageOut;
}
