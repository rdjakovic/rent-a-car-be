package com.nextstep.rentacar.domain.enums;

/**
 * Enumeration representing the possible statuses of a reservation.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    /** The customer has picked the car up; it is out on rental until returned. */
    ACTIVE,
    CANCELLED,
    COMPLETED
}
