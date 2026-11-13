package com.nextstep.rentacar.mapper;

import com.nextstep.rentacar.domain.entity.Maintenance;
import com.nextstep.rentacar.dto.response.MaintenanceResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CarMapper.class})
public interface MaintenanceMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.user.fullName")
    MaintenanceResponseDto toResponseDto(Maintenance maintenance);
}
