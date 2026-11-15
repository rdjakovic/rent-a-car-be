package com.nextstep.rentacar.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    /** Username or email address ("email" is accepted as the field name too, as before) */
    @NotBlank(message = "Username or email is required")
    @Size(max = 255, message = "Username or email should not exceed 255 characters")
    @JsonAlias("email")
    private String username;

    // No length rule here: a sign-in checks the password, password rules apply when setting one
    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password should not exceed 100 characters")
    private String password;
}
