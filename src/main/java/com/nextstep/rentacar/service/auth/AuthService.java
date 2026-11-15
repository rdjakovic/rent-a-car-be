package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.dto.request.LoginRequestDto;
import com.nextstep.rentacar.dto.response.LoginResponseDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import com.nextstep.rentacar.mapper.UserMapper;
import com.nextstep.rentacar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        log.info("User login attempt: {}", loginRequest.getEmail());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Load user entity for JWT token generation
        User user = userRepository.findByUsernameOrEmail(loginRequest.getEmail(), loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate JWT token
        String token = jwtService.generateToken(user);
        OffsetDateTime expiresAt = jwtService.getTokenExpiration(token);
        long expiresIn = jwtService.getTokenExpirationSeconds();

        // Create user response DTO
        UserResponseDto userDto = userMapper.toResponseDto(user);

        log.info("User logged in successfully: {}", user.getUsername());
        return new LoginResponseDto(token, expiresIn, expiresAt, userDto);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(String username) {
        log.debug("Getting current user info: {}", username);

        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return userMapper.toResponseDto(user);
    }
}
