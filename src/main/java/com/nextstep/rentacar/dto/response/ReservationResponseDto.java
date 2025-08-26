package com.nextstep.rentacar.dto.response;

import com.nextstep.rentacar.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDto {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private String currency;
    private String notes;
    private CustomerResponseDto customer;
    private CarListResponseDto car;
    private BranchResponseDto pickupBranch;
    private BranchResponseDto dropoffBranch;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Calculated fields
    private long durationDays;
    private BigDecimal dailyRate;
}
