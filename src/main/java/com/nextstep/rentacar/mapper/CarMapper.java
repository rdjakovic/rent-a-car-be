package com.nextstep.rentacar.mapper;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.dto.request.CarRequestDto;
import com.nextstep.rentacar.dto.response.CarListResponseDto;
import com.nextstep.rentacar.dto.response.CarResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {BranchMapper.class})
public interface CarMapper {

    @Mapping(target = "displayName", source = ".", qualifiedByName = "generateDisplayName")
    CarResponseDto toResponseDto(Car car);

    @Mapping(target = "displayName", source = ".", qualifiedByName = "generateDisplayName")
    @Mapping(target = "branchName", source = "branch.name")
    CarListResponseDto toListResponseDto(Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "maintenanceRecords", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "lastServiceDate", ignore = true)
    @Mapping(target = "nextServiceDate", ignore = true)
    Car toEntity(CarRequestDto carRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "maintenanceRecords", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "lastServiceDate", ignore = true)
    @Mapping(target = "nextServiceDate", ignore = true)
    void updateEntityFromDto(CarRequestDto carRequestDto, @MappingTarget Car car);

    @Named("generateDisplayName")
    default String generateDisplayName(Car car) {
        return car.getYear() + " " + car.getMake() + " " + car.getModel();
    }
}
