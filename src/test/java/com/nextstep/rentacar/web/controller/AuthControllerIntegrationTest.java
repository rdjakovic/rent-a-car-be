package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Employee;
import com.nextstep.rentacar.domain.entity.Role;
import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.repository.RefreshTokenRepository;
import com.nextstep.rentacar.repository.RoleRepository;
import com.nextstep.rentacar.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sign-in, refresh (rotation and reuse), sign-out and password change. Each test signs in its own new user,
 * because failed sign-ins are counted in memory across tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private static final String PASSWORD = "secret-pass-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Sign-in works with username or email and sets the httpOnly refresh cookie")
    void signInWithUsernameOrEmail() throws Exception {
        User user = newUser();

        mockMvc.perform(login(user.getUsername(), PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(notNullValue()))
                .andExpect(jsonPath("$.expires_in").value(60))
                .andExpect(jsonPath("$.user.username").value(user.getUsername()))
                .andExpect(jsonPath("$.user.permissions").isArray())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=43")));

        // The old field name "email" still works
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Staff who work at a branch can sign in and refresh; the user includes the branch")
    void signInWithBranch() throws Exception {
        User user = newUser();
        Branch branch = new Branch();
        branch.setName("Auth Branch");
        branch.setAddress("1 Test St");
        branch.setCity("Test City");
        branch.setCountry("Test Country");
        entityManager.persist(branch);
        Employee employee = new Employee();
        employee.setUser(user);
        employee.setEmployeeId("EMP-" + user.getUsername());
        employee.setBranch(branch);
        entityManager.persist(employee);
        // Sign-in must load the user fresh, as in a real request
        entityManager.flush();
        entityManager.clear();

        Cookie cookie = refreshCookie(mockMvc.perform(login(user.getUsername(), PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.branchName").value("Auth Branch"))
                .andReturn());
        entityManager.clear();
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.branchName").value("Auth Branch"));
    }

    @Test
    @DisplayName("Wrong password, unknown user and disabled account all get the same 401")
    void failedSignInsLookTheSame() throws Exception {
        User user = newUser();
        User disabled = newUser();
        disabled.setEnabled(false);
        userRepository.saveAndFlush(disabled);

        for (var request : new org.springframework.test.web.servlet.RequestBuilder[]{
                login(user.getUsername(), "wrong-password"),
                login("nobody-" + UUID.randomUUID(), PASSWORD),
                login(disabled.getUsername(), PASSWORD)}) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        }
    }

    @Test
    @DisplayName("Five failed attempts lock the account, also for the right password and the email")
    void lockAfterFiveFailures() throws Exception {
        User user = newUser();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login(user.getUsername(), "wrong-password")).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login(user.getEmail(), PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.detail").value(containsString("Too many failed sign-in attempts")));
    }

    @Test
    @DisplayName("Refresh gives a new access token and a new cookie; reusing the old cookie ends every session")
    void refreshRotatesAndDetectsReuse() throws Exception {
        User user = newUser();
        Cookie first = refreshCookie(mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn());

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(notNullValue()))
                .andReturn();
        Cookie second = refreshCookie(refreshed);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        String accessToken = objectMapper.readTree(refreshed.getResponse().getContentAsString()).get("access_token").asString();
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()));

        // The replaced cookie comes back: treated as stolen, so the current one stops working too
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh without a valid session is 401 and deletes the cookie")
    void refreshWithoutSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Your session has ended. Please sign in again."))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(RefreshCookies.NAME, "made-up")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A session ends 12 hours after sign-in, and when the user is disabled")
    void expiredOrDisabled() throws Exception {
        User user = newUser();
        Cookie cookie = refreshCookie(mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn());
        refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .forEach(t -> t.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
        refreshTokenRepository.flush();
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());

        User other = newUser();
        Cookie otherCookie = refreshCookie(mockMvc.perform(login(other.getUsername(), PASSWORD)).andReturn());
        other.setEnabled(false);
        userRepository.saveAndFlush(other);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(otherCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sign-out ends the session and deletes the cookie")
    void logout() throws Exception {
        User user = newUser();
        Cookie cookie = refreshCookie(mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());

        // Signing out without a session is fine
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());

        // A signed-out cookie coming back is not treated as stolen: other sessions go on
        Cookie other = refreshCookie(mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(other)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Changing the password needs the current one and ends the other sessions only")
    void changePassword() throws Exception {
        User user = newUser();
        MvcResult here = mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn();
        Cookie hereCookie = refreshCookie(here);
        Cookie elsewhere = refreshCookie(mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn());
        String accessToken = objectMapper.readTree(here.getResponse().getContentAsString()).get("access_token").asString();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).cookie(hereCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "wrong-password", "newPassword", "new-secret-2"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).cookie(hereCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "new-secret-2"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(login(user.getUsername(), PASSWORD)).andExpect(status().isUnauthorized());
        mockMvc.perform(login(user.getUsername(), "new-secret-2")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(elsewhere)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(hereCookie)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Changing the password needs a signed-in user and a valid new password")
    void changePasswordValidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "new-secret-2"))))
                .andExpect(status().isUnauthorized());

        User user = newUser();
        MvcResult signedIn = mockMvc.perform(login(user.getUsername(), PASSWORD)).andReturn();
        String accessToken = objectMapper.readTree(signedIn.getResponse().getContentAsString()).get("access_token").asString();
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "short"))))
                .andExpect(status().isBadRequest());
    }

    private User newUser() {
        String name = "user-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setRoles(Set.of(staffRole()));
        return userRepository.saveAndFlush(user);
    }

    /** The test profile's schema comes from Hibernate, without Flyway's seeded roles */
    private Role staffRole() {
        return roleRepository.findByName("STAFF").orElseGet(() -> {
            Role role = new Role();
            role.setName("STAFF");
            return roleRepository.saveAndFlush(role);
        });
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String username, String password) throws Exception {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password)));
    }

    private String json(Map<String, String> body) {
        return objectMapper.writeValueAsString(body);
    }

    private static Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(RefreshCookies.NAME);
        assertThat(cookie).as("refresh cookie").isNotNull();
        return new Cookie(cookie.getName(), cookie.getValue());
    }
}
