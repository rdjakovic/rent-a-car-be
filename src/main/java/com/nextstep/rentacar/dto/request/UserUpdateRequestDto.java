package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Replaces a user's details (the username never changes). Passwords are set with the reset-password endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email should not exceed 255 characters")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name should not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name should not exceed 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone should not exceed 20 characters")
    private String phone;

    @NotNull(message = "Role is required")
    private UserRole role;

    /** The branch the user works at: required for STAFF and MANAGER */
    private Long branchId;

    /** A disabled user can't sign in, and their sessions end at once */
    @NotNull(message = "Enabled is required")
    private Boolean enabled;
}
