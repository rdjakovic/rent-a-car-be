package com.nextstep.rentacar.testutil.builders;

import com.nextstep.rentacar.dto.request.BranchRequestDto;

/**
 * Test data builder for Branch-related DTOs and entities.
 * Uses fluent builder pattern to reduce test boilerplate.
 */
public class BranchTestDataBuilder {
    
    private String name = "Test Branch " + System.currentTimeMillis() % 10000;
    private String address = "123 Main St";
    private String city = "Test City";
    private String country = "Test Country";
    private String phoneNumber = "+1555" + String.format("%07d", System.currentTimeMillis() % 10000000);
    private String email = "branch" + System.currentTimeMillis() % 10000 + "@test.com";
    private String operatingHours = "Mon-Fri 9:00-18:00";
    private Boolean active = true;
    
    private BranchTestDataBuilder() {
    }
    
    public static BranchTestDataBuilder aBranch() {
        return new BranchTestDataBuilder();
    }
    
    public BranchTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public BranchTestDataBuilder withAddress(String address) {
        this.address = address;
        return this;
    }
    
    public BranchTestDataBuilder withCity(String city) {
        this.city = city;
        return this;
    }
    
    public BranchTestDataBuilder inNewYork() {
        this.city = "New York";
        this.country = "USA";
        this.address = "5th Avenue 123";
        return this;
    }
    
    public BranchTestDataBuilder inLosAngeles() {
        this.city = "Los Angeles";
        this.country = "USA";
        this.address = "Sunset Blvd 456";
        return this;
    }
    
    public BranchTestDataBuilder inLondon() {
        this.city = "London";
        this.country = "UK";
        this.address = "Oxford Street 789";
        this.phoneNumber = "+442071234567";
        return this;
    }
    
    public BranchTestDataBuilder withCountry(String country) {
        this.country = country;
        return this;
    }
    
    public BranchTestDataBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }
    
    public BranchTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public BranchTestDataBuilder withOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
        return this;
    }
    
    public BranchTestDataBuilder open24Hours() {
        this.operatingHours = "24/7";
        return this;
    }
    
    public BranchTestDataBuilder withActive(Boolean active) {
        this.active = active;
        return this;
    }
    
    public BranchTestDataBuilder inactive() {
        this.active = false;
        return this;
    }
    
    public BranchRequestDto build() {
        return new BranchRequestDto(
            name,
            address,
            city,
            country,
            phoneNumber,
            email,
            operatingHours,
            active
        );
    }
}
