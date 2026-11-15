package com.nextstep.rentacar.service;

import com.nextstep.rentacar.domain.enums.UserRole;
import com.nextstep.rentacar.dto.request.ResetPasswordRequestDto;
import com.nextstep.rentacar.dto.request.UserCreateRequestDto;
import com.nextstep.rentacar.dto.request.UserUpdateRequestDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

/**
 * User account management. The {@code caller} is the signed-in user doing it.
 */
public interface UserService {

    /** Users whose name, username or email contains {@code search}, optionally with this role (both optional). */
    Page<UserResponseDto> search(String search, UserRole role, Pageable pageable);

    UserResponseDto getById(Long id);

    /** Managers may only create STAFF accounts; managers and administrators need USER_MANAGE. */
    UserResponseDto create(UserCreateRequestDto request, Authentication caller);

    /** An administrator can't disable or demote their own account. */
    UserResponseDto update(Long id, UserUpdateRequestDto request, Authentication caller);

    /** Sets a new password and ends all of the user's sessions (also lifts a sign-in lock). */
    void resetPassword(Long id, ResetPasswordRequestDto request);
}
