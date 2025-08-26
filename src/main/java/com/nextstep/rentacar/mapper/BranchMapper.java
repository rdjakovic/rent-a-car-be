package com.nextstep.rentacar.mapper;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.dto.request.BranchRequestDto;
import com.nextstep.rentacar.dto.response.BranchResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    BranchResponseDto toResponseDto(Branch branch);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "cars", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "pickupReservations", ignore = true)
    @Mapping(target = "dropoffReservations", ignore = true)
    Branch toEntity(BranchRequestDto branchRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "cars", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "pickupReservations", ignore = true)
    @Mapping(target = "dropoffReservations", ignore = true)
    void updateEntityFromDto(BranchRequestDto branchRequestDto, @MappingTarget Branch branch);
}
