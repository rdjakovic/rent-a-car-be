package com.nextstep.rentacar.domain.enums;

/**
 * The roles a user account can have (each account has exactly one). What each role may do is defined in
 * {@code RolePermissions}.
 */
public enum UserRole {
    STAFF,
    MANAGER,
    ADMIN
}
