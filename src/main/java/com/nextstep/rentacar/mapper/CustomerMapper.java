package com.nextstep.rentacar.mapper;

import com.nextstep.rentacar.domain.entity.Customer;
import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import com.nextstep.rentacar.dto.response.CustomerResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "fullName", source = ".", qualifiedByName = "generateFullName")
    CustomerResponseDto toResponseDto(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Customer toEntity(CustomerRequestDto customerRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    void updateEntityFromDto(CustomerRequestDto customerRequestDto, @MappingTarget Customer customer);

    @Named("generateFullName")
    default String generateFullName(Customer customer) {
        return customer.getFirstName() + " " + customer.getLastName();
    }
}
