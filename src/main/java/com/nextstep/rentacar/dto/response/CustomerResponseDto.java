package com.nextstep.rentacar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String driverLicenseNo;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String country;
    private LocalDate licenseExpiryDate;
    private String fullName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
