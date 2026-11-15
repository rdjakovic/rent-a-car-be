package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.dto.request.ChangePasswordRequestDto;
import com.nextstep.rentacar.dto.request.LoginRequestDto;
import com.nextstep.rentacar.dto.response.LoginResponseDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import com.nextstep.rentacar.exception.InvalidCredentialsException;
import com.nextstep.rentacar.exception.SessionExpiredException;
import com.nextstep.rentacar.mapper.UserMapper;
import com.nextstep.rentacar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** The response body plus the refresh token, which the controller puts in a cookie */
    public record SignedIn(LoginResponseDto response, String refreshToken, OffsetDateTime sessionEndsAt) {
    }

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public SignedIn login(LoginRequestDto loginRequest) {
        String login = loginRequest.getUsername().trim();
        Optional<User> account = userRepository.findByUsernameOrEmail(login, login);
        // Count failures per account, so switching between username and email doesn't reset them
        String attemptsKey = account.map(u -> "user:" + u.getId()).orElse("login:" + login.toLowerCase(Locale.ROOT));
        loginAttemptService.checkNotLocked(attemptsKey);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            loginAttemptService.failed(attemptsKey);
            log.info("Login failed for: {} ({})", login, e.getClass().getSimpleName());
            throw new InvalidCredentialsException();
        }
        loginAttemptService.succeeded(attemptsKey);

        User user = account.orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("User logged in successfully: {}", user.getUsername());
        return signedIn(refreshTokenService.start(user));
    }

    /**
     * A new access token (and refresh token) for the session of this refresh token.
     * A failed refresh must not roll back: a detected token reuse ends the user's sessions for good.
     */
    @Transactional(noRollbackFor = SessionExpiredException.class)
    public SignedIn refresh(String refreshToken) {
        return signedIn(refreshTokenService.rotate(refreshToken));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.end(refreshToken);
        }
    }

    /** Changes the signed-in user's password and ends their other sessions (this one stays). */
    @Transactional
    public void changePassword(String username, ChangePasswordRequestDto request, String currentRefreshToken) {
        User user = findUser(username);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            // 400, not 401: the session is fine, only the entered password is wrong
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("The new password must be different from the current one");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.endOtherSessions(user, currentRefreshToken);
        log.info("Password changed for: {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(String username) {
        log.debug("Getting current user info: {}", username);
        return userMapper.toResponseDto(findUser(username));
    }

    private User findUser(String username) {
        return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private SignedIn signedIn(RefreshTokenService.Session session) {
        User user = session.user();
        String accessToken = jwtService.generateToken(user);
        LoginResponseDto response = new LoginResponseDto(accessToken, jwtService.getTokenExpirationSeconds(),
                jwtService.getTokenExpiration(accessToken), userMapper.toResponseDto(user));
        return new SignedIn(response, session.token(), session.expiresAt());
    }
}
