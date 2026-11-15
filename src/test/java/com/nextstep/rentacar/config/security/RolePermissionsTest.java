package com.nextstep.rentacar.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nextstep.rentacar.config.security.Permission.*;
import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionsTest {

    @Test
    @DisplayName("Each role has the rights of the one before it")
    void rolesBuildOnEachOther() {
        var staff = RolePermissions.permissionsOf(List.of("STAFF"));
        var manager = RolePermissions.permissionsOf(List.of("MANAGER"));
        var admin = RolePermissions.permissionsOf(List.of("ADMIN"));

        assertThat(staff).containsExactlyInAnyOrder(CUSTOMER_MANAGE, RESERVATION_MANAGE, MAINTENANCE_SCHEDULE);
        assertThat(manager).containsAll(staff)
                .contains(CUSTOMER_DELETE, CAR_MANAGE, MAINTENANCE_MANAGE, USER_VIEW, USER_CREATE_STAFF)
                .doesNotContain(CAR_DELETE, BRANCH_MANAGE, SETTINGS_MANAGE, USER_MANAGE);
        assertThat(admin).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    @DisplayName("Unknown roles grant nothing; several roles add up")
    void unknownAndCombinedRoles() {
        assertThat(RolePermissions.permissionsOf(List.of("CUSTOMER"))).isEmpty();
        assertThat(RolePermissions.permissionsOf(List.of("STAFF", "CUSTOMER")))
                .isEqualTo(RolePermissions.permissionsOf(List.of("STAFF")));
    }

    @Test
    @DisplayName("Authorities are ROLE_<role> plus the permission names")
    void authorities() {
        assertThat(RolePermissions.authoritiesOf(List.of("STAFF")))
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_STAFF", "CUSTOMER_MANAGE", "RESERVATION_MANAGE", "MAINTENANCE_SCHEDULE");
    }
}
