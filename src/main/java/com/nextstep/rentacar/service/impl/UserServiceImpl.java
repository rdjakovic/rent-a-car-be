package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.config.security.Permission;
import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Employee;
import com.nextstep.rentacar.domain.entity.Role;
import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.domain.enums.UserRole;
import com.nextstep.rentacar.dto.request.ResetPasswordRequestDto;
import com.nextstep.rentacar.dto.request.UserCreateRequestDto;
import com.nextstep.rentacar.dto.request.UserUpdateRequestDto;
import com.nextstep.rentacar.dto.response.UserResponseDto;
import com.nextstep.rentacar.exception.DuplicateResourceException;
import com.nextstep.rentacar.mapper.UserMapper;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.repository.EmployeeRepository;
import com.nextstep.rentacar.repository.RoleRepository;
import com.nextstep.rentacar.repository.UserRepository;
import com.nextstep.rentacar.service.UserService;
import com.nextstep.rentacar.service.auth.LoginAttemptService;
import com.nextstep.rentacar.service.auth.RefreshTokenService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> search(String search, UserRole role, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.join("roles").get("name"), role.name()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, pageable).map(userMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        return userMapper.toResponseDto(find(id));
    }

    @Override
    public UserResponseDto create(UserCreateRequestDto request, Authentication caller) {
        if (request.getRole() != UserRole.STAFF && !has(caller, Permission.USER_MANAGE)) {
            throw new AccessDeniedException("Only administrators can create managers and administrators");
        }
        requireBranchForWorkRoles(request.getRole(), request.getBranchId());

        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setEnabled(true);
        user.setRoles(roleSet(request.getRole()));
        User saved = userRepository.save(user);

        if (request.getBranchId() != null) {
            assignBranch(saved, request.getBranchId());
        }
        log.info("User {} created by {} with role {}", saved.getUsername(), caller == null ? "?" : caller.getName(), request.getRole());
        return userMapper.toResponseDto(saved);
    }

    @Override
    public UserResponseDto update(Long id, UserUpdateRequestDto request, Authentication caller) {
        User user = find(id);
        boolean self = caller != null && user.getUsername().equals(caller.getName());
        if (self && (!request.getEnabled() || request.getRole() != UserRole.ADMIN)) {
            throw new IllegalStateException("You can't disable or demote your own account");
        }
        requireBranchForWorkRoles(request.getRole(), request.getBranchId());

        String email = normalizeEmail(request.getEmail());
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use");
        }

        boolean disabling = Boolean.TRUE.equals(user.getEnabled()) && !request.getEnabled();
        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setRoles(roleSet(request.getRole()));
        user.setEnabled(request.getEnabled());
        if (request.getBranchId() != null) {
            assignBranch(user, request.getBranchId());
        }
        if (disabling) {
            refreshTokenService.endAllSessions(user);
        }
        userRepository.flush();
        log.info("User {} updated by {}", user.getUsername(), caller == null ? "?" : caller.getName());
        return userMapper.toResponseDto(user);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordRequestDto request) {
        User user = find(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.endAllSessions(user);
        loginAttemptService.succeeded(LoginAttemptService.userKey(user.getId()));
        log.info("Password reset for {}", user.getUsername());
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    private static boolean has(Authentication caller, Permission permission) {
        return caller != null && caller.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission.name()));
    }

    private static void requireBranchForWorkRoles(UserRole role, Long branchId) {
        if (role != UserRole.ADMIN && branchId == null) {
            throw new IllegalArgumentException("Branch is required for staff and managers");
        }
    }

    private Set<Role> roleSet(UserRole role) {
        return new HashSet<>(Set.of(roleRepository.findByName(role.name())
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + role))));
    }

    /** Puts the user at the branch, creating their employee record if they have none yet. */
    private void assignBranch(User user, Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
        Employee employee = user.getEmployee();
        if (employee == null) {
            employee = new Employee();
            employee.setUser(user);
            employee.setEmployeeId(nextEmployeeId(user.getId()));
            employee.setHireDate(LocalDate.now());
            employee.setIsActive(true);
            user.setEmployee(employee);
        }
        employee.setBranch(branch);
        employeeRepository.save(employee);
    }

    /** EMP + the user's id (at least 3 digits), moving on if that number is already taken. */
    private String nextEmployeeId(Long userId) {
        long number = userId;
        String employeeId;
        do {
            employeeId = String.format("EMP%03d", number++);
        } while (employeeRepository.existsByEmployeeId(employeeId));
        return employeeId;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
