package com.nextstep.rentacar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean enabled;
    private Set<String> roles;
    /** What the user may do (see Permission); the UI shows only the actions these allow */
    private Set<String> permissions;
    /** The branch the user works at (staff and managers); null for users without one */
    private Long branchId;
    private String branchName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
