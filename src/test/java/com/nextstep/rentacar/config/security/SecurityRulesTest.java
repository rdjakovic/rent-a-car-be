package com.nextstep.rentacar.config.security;

import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.repository.UserRepository;
import com.nextstep.rentacar.service.auth.JwtService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the production security filter chain (active for any profile other than local/test).
 * Allowed requests may still fail for other reasons (400 validation, 404 unknown id); the tests only
 * check whether security lets them through. Mutating requests use empty bodies or unknown ids.
 * Without a (valid) token the answer is 401; signed in without the right permission it is 403.
 */
@SpringBootTest(properties = "app.jwt.secret=security-rules-test-secret-at-least-32-chars")
@AutoConfigureMockMvc
@ActiveProfiles("security-rules-test")
class SecurityRulesTest {

    private static final Set<String> ALL = Set.of("STAFF", "MANAGER", "ADMIN");
    private static final Set<String> MANAGERS = Set.of("MANAGER", "ADMIN");
    private static final Set<String> ADMIN = Set.of("ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    /** method, path, roles allowed through security */
    static Stream<Arguments> rules() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/branches", ALL),
                Arguments.of("POST", "/api/v1/branches", ADMIN),
                Arguments.of("PUT", "/api/v1/branches/999999", ADMIN),
                Arguments.of("DELETE", "/api/v1/branches/999999", ADMIN),

                Arguments.of("GET", "/api/v1/cars", ALL),
                Arguments.of("GET", "/api/v1/cars/available", ALL),
                Arguments.of("GET", "/api/v1/cars/deleted", MANAGERS),
                Arguments.of("POST", "/api/v1/cars", MANAGERS),
                Arguments.of("PUT", "/api/v1/cars/999999", MANAGERS),
                Arguments.of("DELETE", "/api/v1/cars/999999", ADMIN),
                Arguments.of("POST", "/api/v1/cars/999999/restore", ADMIN),

                Arguments.of("GET", "/api/v1/customers", ALL),
                Arguments.of("POST", "/api/v1/customers", ALL),
                Arguments.of("PUT", "/api/v1/customers/999999", ALL),
                Arguments.of("DELETE", "/api/v1/customers/999999", MANAGERS),

                Arguments.of("GET", "/api/v1/reservations", ALL),
                Arguments.of("POST", "/api/v1/reservations", ALL),
                Arguments.of("PUT", "/api/v1/reservations/999999", ALL),
                Arguments.of("POST", "/api/v1/reservations/999999/confirm", ALL),
                Arguments.of("POST", "/api/v1/reservations/999999/cancel", ALL),
                Arguments.of("POST", "/api/v1/reservations/999999/pickup", ALL),
                Arguments.of("POST", "/api/v1/reservations/999999/complete", ALL),

                Arguments.of("GET", "/api/v1/maintenance", ALL),
                Arguments.of("GET", "/api/v1/maintenance/999999", ALL),
                Arguments.of("GET", "/api/v1/maintenance/due-cars", ALL),
                Arguments.of("POST", "/api/v1/maintenance/schedule", ALL),
                Arguments.of("PUT", "/api/v1/maintenance/999999", MANAGERS),
                Arguments.of("POST", "/api/v1/maintenance/999999/start", MANAGERS),
                Arguments.of("POST", "/api/v1/maintenance/999999/complete", MANAGERS),
                Arguments.of("POST", "/api/v1/maintenance/999999/cancel", MANAGERS),

                Arguments.of("GET", "/api/v1/settings", ALL),
                Arguments.of("PUT", "/api/v1/settings", ADMIN)
        );
    }

    static Stream<Arguments> rulesPerPrincipal() {
        return rules().flatMap(rule -> {
            Object[] r = rule.get();
            @SuppressWarnings("unchecked")
            Set<String> allowed = (Set<String>) r[2];
            return Stream.concat(
                    Stream.of(Arguments.of(r[0], r[1], "ANONYMOUS", false)),
                    ALL.stream().sorted().map(role -> Arguments.of(r[0], r[1], role, allowed.contains(role))));
        });
    }

    @ParameterizedTest(name = "{0} {1} as {2} -> allowed={3}")
    @MethodSource("rulesPerPrincipal")
    void roleRules(String method, String path, String principal, boolean allowed) throws Exception {
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        if (!"ANONYMOUS".equals(principal)) {
            req = req.with(user("test-" + principal.toLowerCase())
                    .authorities(RolePermissions.authoritiesOf(List.of(principal))));
        }

        int status = mockMvc.perform(req).andReturn().getResponse().getStatus();

        if ("ANONYMOUS".equals(principal)) {
            assertThat(status).as("%s %s without a token should be 401", method, path).isEqualTo(401);
        } else if (allowed) {
            assertThat(status).as("%s %s as %s should pass security", method, path, principal).isNotIn(401, 403);
        } else {
            assertThat(status).as("%s %s as %s should be forbidden", method, path, principal).isEqualTo(403);
        }
    }

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public registration is gone: accounts are created by admins")
    void registrationIsGone() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Without a token the answer is a 401 problem detail")
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("A missing permission is a 403 problem detail")
    void forbiddenIsAProblemDetail() throws Exception {
        mockMvc.perform(put("/api/v1/settings").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(user("staff").authorities(RolePermissions.authoritiesOf(List.of("STAFF")))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("You don't have permission for this action"));
    }

    @Test
    @DisplayName("An admin's JWT grants every permission, and /me lists them")
    void validJwtGrantsTheUsersRoles() throws Exception {
        User admin = userRepository.findByUsernameWithRoles("admin").orElseThrow();
        String token = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value(hasItem("ADMIN")))
                .andExpect(jsonPath("$.permissions").value(hasItem("SETTINGS_MANAGE")));
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The former employee is STAFF: reservations yes, adding cars no; /me shows the branch")
    void staffJwtHasStaffRights() throws Exception {
        User staff = userRepository.findByUsernameWithRoles("john.doe").orElseThrow();
        String token = jwtService.generateToken(staff);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value(hasItem("STAFF")))
                .andExpect(jsonPath("$.permissions").value(hasItem("RESERVATION_MANAGE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("CAR_MANAGE"))))
                .andExpect(jsonPath("$.branchId").value(1));
        mockMvc.perform(get("/api/v1/reservations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cars").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("A disabled user's token stops working before it expires")
    void disabledUsersTokenIsRefused() throws Exception {
        User staff = userRepository.findByUsernameWithRoles("john.doe").orElseThrow();
        String token = jwtService.generateToken(staff);
        staff.setEnabled(false);
        userRepository.saveAndFlush(staff);

        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtSignedWithAnotherKeyIsRejected() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("1")
                .issuer("rent-a-car-app")
                .audience("rent-a-car-users")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .claim("roles", List.of("ADMIN"))
                .build();
        SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        forged.sign(new MACSigner("some-other-secret-that-is-at-least-32-chars"));

        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + forged.serialize()))
                .andExpect(status().isUnauthorized());
    }
}
