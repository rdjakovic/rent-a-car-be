package com.nextstep.rentacar.mapper;

import com.nextstep.rentacar.config.security.RolePermissions;
import com.nextstep.rentacar.domain.entity.Role;
import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStrings")
    @Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissions")
    @Mapping(target = "branchId", source = "employee.branch.id")
    @Mapping(target = "branchName", source = "employee.branch.name")
    UserResponseDto toResponseDto(User user);

    @Named("rolesToStrings")
    default Set<String> rolesToStrings(Set<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    @Named("rolesToPermissions")
    default Set<String> rolesToPermissions(Set<Role> roles) {
        return RolePermissions.permissionsOf(rolesToStrings(roles)).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
