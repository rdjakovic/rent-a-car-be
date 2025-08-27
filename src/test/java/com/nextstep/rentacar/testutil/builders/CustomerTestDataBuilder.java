package com.nextstep.rentacar.testutil.builders;

import com.nextstep.rentacar.dto.request.CustomerRequestDto;
import java.time.LocalDate;

/**
 * Test data builder for Customer-related DTOs and entities.
 * Uses fluent builder pattern to reduce test boilerplate.
 */
public class CustomerTestDataBuilder {
    
    private String firstName = "John";
    private String lastName = "Doe";
    private String email = "john.doe" + System.currentTimeMillis() % 100000 + "@test.com";
    private String phone = "+1555" + String.format("%07d", System.currentTimeMillis() % 10000000);
    private String driverLicenseNo = "DL" + System.currentTimeMillis() % 100000000;
    private LocalDate dateOfBirth = LocalDate.now().minusYears(30);
    private String address = "123 Test Street";
    private String city = "Test City";
    private String country = "USA";
    private LocalDate licenseExpiryDate = LocalDate.now().plusYears(3);
    
    private CustomerTestDataBuilder() {
    }
    
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    public CustomerTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }
    
    public CustomerTestDataBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }
    
    public CustomerTestDataBuilder named(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        return this;
    }
    
    public CustomerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public CustomerTestDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }
    
    public CustomerTestDataBuilder withDriverLicenseNo(String driverLicenseNo) {
        this.driverLicenseNo = driverLicenseNo;
        return this;
    }
    
    public CustomerTestDataBuilder withDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }
    
    public CustomerTestDataBuilder aged(int years) {
        this.dateOfBirth = LocalDate.now().minusYears(years);
        return this;
    }
    
    public CustomerTestDataBuilder youngDriver() {
        this.dateOfBirth = LocalDate.now().minusYears(21);
        return this;
    }
    
    public CustomerTestDataBuilder seniorDriver() {
        this.dateOfBirth = LocalDate.now().minusYears(65);
        return this;
    }
    
    public CustomerTestDataBuilder withAddress(String address) {
        this.address = address;
        return this;
    }
    
    public CustomerTestDataBuilder withCity(String city) {
        this.city = city;
        return this;
    }
    
    public CustomerTestDataBuilder fromNewYork() {
        this.city = "New York";
        this.country = "USA";
        this.address = "5th Avenue 123";
        return this;
    }
    
    public CustomerTestDataBuilder fromLondon() {
        this.city = "London";
        this.country = "UK";
        this.address = "Oxford Street 456";
        this.phone = "+442071234567";
        return this;
    }
    
    public CustomerTestDataBuilder withCountry(String country) {
        this.country = country;
        return this;
    }
    
    public CustomerTestDataBuilder withLicenseExpiryDate(LocalDate licenseExpiryDate) {
        this.licenseExpiryDate = licenseExpiryDate;
        return this;
    }
    
    public CustomerTestDataBuilder withExpiredLicense() {
        this.licenseExpiryDate = LocalDate.now().minusDays(1);
        return this;
    }
    
    public CustomerTestDataBuilder withLicenseExpiringInDays(int days) {
        this.licenseExpiryDate = LocalDate.now().plusDays(days);
        return this;
    }
    
    public CustomerRequestDto build() {
        CustomerRequestDto dto = new CustomerRequestDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setDriverLicenseNo(driverLicenseNo);
        dto.setDateOfBirth(dateOfBirth);
        dto.setAddress(address);
        dto.setCity(city);
        dto.setCountry(country);
        dto.setLicenseExpiryDate(licenseExpiryDate);
        return dto;
    }
}
