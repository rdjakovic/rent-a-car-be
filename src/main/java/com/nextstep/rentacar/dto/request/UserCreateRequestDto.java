package com.nextstep.rentacar.dto.request;

import com.nextstep.rentacar.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequestDto {

    // No '@': sign-in accepts a username or an email, which must never be ambiguous
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may only contain letters, digits, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email should not exceed 255 characters")
    private String email;

    /** The initial password; the user can change it after signing in */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

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
}
