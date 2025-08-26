package com.nextstep.rentacar.service.auth;

import com.nextstep.rentacar.domain.entity.Role;
import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.dto.request.LoginRequestDto;
import com.nextstep.rentacar.dto.request.RegisterRequestDto;
import com.nextstep.rentacar.dto.response.LoginResponseDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import com.nextstep.rentacar.mapper.UserMapper;
import com.nextstep.rentacar.repository.RoleRepository;
import com.nextstep.rentacar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto register(RegisterRequestDto registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create user entity
        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Assign default CUSTOMER role
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default CUSTOMER role not found"));
        user.setRoles(Set.of(customerRole));

        // Save user
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully: {}", savedUser.getUsername());
        return userMapper.toResponseDto(savedUser);
    }

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
