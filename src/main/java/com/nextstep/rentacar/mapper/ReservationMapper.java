package com.nextstep.rentacar.mapper;

import com.nextstep.rentacar.domain.entity.Reservation;
import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import com.nextstep.rentacar.dto.response.ReservationResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, CarMapper.class, BranchMapper.class})
public interface ReservationMapper {

    @Mapping(target = "durationDays", source = ".", qualifiedByName = "calculateDurationDays")
    @Mapping(target = "dailyRate", source = "car.dailyPrice")
    ReservationResponseDto toResponseDto(Reservation reservation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "pickupBranch", ignore = true)
    @Mapping(target = "dropoffBranch", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "payments", ignore = true)
    Reservation toEntity(ReservationRequestDto reservationRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "pickupBranch", ignore = true)
    @Mapping(target = "dropoffBranch", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "payments", ignore = true)
    void updateEntityFromDto(ReservationRequestDto reservationRequestDto, @MappingTarget Reservation reservation);

    @Named("calculateDurationDays")
    default long calculateDurationDays(Reservation reservation) {
        LocalDate startDate = reservation.getStartDate();
        LocalDate endDate = reservation.getEndDate();
        
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
}
