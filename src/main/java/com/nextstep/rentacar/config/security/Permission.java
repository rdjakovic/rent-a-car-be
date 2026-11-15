package com.nextstep.rentacar.config.security;

/**
 * What a signed-in user may do. Roles grant permissions ({@link RolePermissions}), the security rules
 * check permissions, and {@code GET /auth/me} returns them so the UI can show only what the user may use.
 * Browsing (cars, branches, settings) only needs a signed-in user.
 */
public enum Permission {

    /** See, create and edit customers */
    CUSTOMER_MANAGE,
    CUSTOMER_DELETE,

    /** Everything about reservations: book, edit, confirm, cancel, pick up, return */
    RESERVATION_MANAGE,

    /** See maintenance jobs and schedule new ones */
    MAINTENANCE_SCHEDULE,
    /** Edit, start, complete and cancel maintenance jobs */
    MAINTENANCE_MANAGE,

    /** Add and edit cars, see deleted cars */
    CAR_MANAGE,
    /** Delete and restore cars */
    CAR_DELETE,

    /** Create, edit, open and close branches */
    BRANCH_MANAGE,

    /** Change the application settings */
    SETTINGS_MANAGE,

    /** See the user accounts */
    USER_VIEW,
    /** Create STAFF accounts (creating managers and administrators needs USER_MANAGE) */
    USER_CREATE_STAFF,
    /** Everything else about accounts: roles, branch, disabling, passwords */
    USER_MANAGE
}
