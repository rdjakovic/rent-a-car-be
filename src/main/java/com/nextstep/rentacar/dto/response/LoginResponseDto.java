package com.nextstep.rentacar.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("expires_at")
    private OffsetDateTime expiresAt;

    private UserResponseDto user;

    public LoginResponseDto(String accessToken, long expiresIn, OffsetDateTime expiresAt, UserResponseDto user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.user = user;
    }
}
