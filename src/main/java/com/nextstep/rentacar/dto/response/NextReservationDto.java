package com.nextstep.rentacar.dto.response;

import com.nextstep.rentacar.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * The car's current or next booking: an open reservation (PENDING, CONFIRMED or ACTIVE) that has not
 * ended yet, or a rental still out past its end date. The car's status only says whether it is on the
 * lot now; this says when it is taken.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextReservationDto {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReservationStatus status;
}
