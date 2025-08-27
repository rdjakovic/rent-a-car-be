package com.nextstep.rentacar.testutil.builders;

import com.nextstep.rentacar.dto.request.ReservationRequestDto;
import java.time.LocalDate;

/**
 * Test data builder for Reservation-related DTOs and entities.
 * Uses fluent builder pattern to reduce test boilerplate.
 */
public class ReservationTestDataBuilder {
    
    private Long customerId;
    private Long carId;
    private LocalDate startDate = LocalDate.now().plusDays(7);
    private LocalDate endDate = LocalDate.now().plusDays(14);
    private Long pickupBranchId;
    private Long returnBranchId;
    private String notes = "Test reservation";
    
    private ReservationTestDataBuilder() {
    }
    
    public static ReservationTestDataBuilder aReservation() {
        return new ReservationTestDataBuilder();
    }
    
    public ReservationTestDataBuilder forCustomer(Long customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public ReservationTestDataBuilder forCar(Long carId) {
        this.carId = carId;
        return this;
    }
    
    public ReservationTestDataBuilder withStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }
    
    public ReservationTestDataBuilder withEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }
    
    public ReservationTestDataBuilder withDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }
    
    public ReservationTestDataBuilder startingToday() {
        this.startDate = LocalDate.now();
        return this;
    }
    
    public ReservationTestDataBuilder startingTomorrow() {
        this.startDate = LocalDate.now().plusDays(1);
        return this;
    }
    
    public ReservationTestDataBuilder forDays(int days) {
        this.endDate = this.startDate.plusDays(days);
        return this;
    }
    
    public ReservationTestDataBuilder forOneWeek() {
        this.endDate = this.startDate.plusDays(7);
        return this;
    }
    
    public ReservationTestDataBuilder forOneMonth() {
        this.endDate = this.startDate.plusMonths(1);
        return this;
    }
    
    public ReservationTestDataBuilder forWeekend() {
        // Assuming starting Friday, ending Sunday
        this.startDate = LocalDate.now().plusDays((5 - LocalDate.now().getDayOfWeek().getValue() + 7) % 7);
        this.endDate = this.startDate.plusDays(2);
        return this;
    }
    
    public ReservationTestDataBuilder withPickupBranch(Long pickupBranchId) {
        this.pickupBranchId = pickupBranchId;
        this.returnBranchId = pickupBranchId; // Default to same branch
        return this;
    }
    
    public ReservationTestDataBuilder withReturnBranch(Long returnBranchId) {
        this.returnBranchId = returnBranchId;
        return this;
    }
    
    public ReservationTestDataBuilder withBranches(Long pickupBranchId, Long returnBranchId) {
        this.pickupBranchId = pickupBranchId;
        this.returnBranchId = returnBranchId;
        return this;
    }
    
    public ReservationTestDataBuilder withSameBranch(Long branchId) {
        this.pickupBranchId = branchId;
        this.returnBranchId = branchId;
        return this;
    }
    
    public ReservationTestDataBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }
    
    public ReservationRequestDto build() {
        return new ReservationRequestDto(
            customerId,
            carId,
            startDate,
            endDate,
            pickupBranchId,
            returnBranchId,
            notes
        );
    }
}
