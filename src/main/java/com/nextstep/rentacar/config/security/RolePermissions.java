package com.nextstep.rentacar.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nextstep.rentacar.config.security.Permission.*;

/**
 * The permissions of each role. Each role has the rights of the one before it: STAFF, MANAGER, ADMIN.
 */
public final class RolePermissions {

    public static final String STAFF = "STAFF";
    public static final String MANAGER = "MANAGER";
    public static final String ADMIN = "ADMIN";

    private static final Set<Permission> STAFF_PERMISSIONS =
            EnumSet.of(CUSTOMER_MANAGE, RESERVATION_MANAGE, MAINTENANCE_SCHEDULE);

    private static final Set<Permission> MANAGER_PERMISSIONS = with(STAFF_PERMISSIONS,
            CUSTOMER_DELETE, CAR_MANAGE, MAINTENANCE_MANAGE);

    // Admins can do everything, including permissions added later
    private static final Set<Permission> ADMIN_PERMISSIONS = EnumSet.allOf(Permission.class);

    private static final Map<String, Set<Permission>> BY_ROLE = Map.of(
            STAFF, STAFF_PERMISSIONS,
            MANAGER, MANAGER_PERMISSIONS,
            ADMIN, ADMIN_PERMISSIONS);

    private RolePermissions() {
    }

    /** All permissions of the given roles; unknown roles grant none. */
    public static Set<Permission> permissionsOf(Collection<String> roles) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> permissions.addAll(BY_ROLE.getOrDefault(role, Set.of())));
        return permissions;
    }

    /** Spring Security authorities: ROLE_&lt;role&gt; for each role plus each permission's name. */
    public static List<GrantedAuthority> authoritiesOf(Collection<String> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissionsOf(roles).forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.name())));
        return authorities;
    }

    private static Set<Permission> with(Set<Permission> base, Permission... more) {
        Set<Permission> permissions = EnumSet.copyOf(base);
        permissions.addAll(List.of(more));
        return permissions;
    }
}
