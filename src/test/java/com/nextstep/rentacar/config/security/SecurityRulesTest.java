package com.nextstep.rentacar.config.security;

import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.repository.UserRepository;
import com.nextstep.rentacar.service.auth.JwtService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the production security filter chain (active for any profile other than local/test).
 * Allowed requests may still fail for other reasons (400 validation, 404 unknown id); the tests only
 * check whether security lets them through. Mutating requests use empty bodies or unknown ids.
 */
@SpringBootTest(properties = "app.jwt.secret=security-rules-test-secret-at-least-32-chars")
@AutoConfigureMockMvc
@ActiveProfiles("security-rules-test")
class SecurityRulesTest {

    private static final Set<String> ALL = Set.of("CUSTOMER", "EMPLOYEE", "ADMIN");
    private static final Set<String> STAFF = Set.of("EMPLOYEE", "ADMIN");
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
                Arguments.of("GET", "/api/v1/cars/deleted", STAFF),
                Arguments.of("POST", "/api/v1/cars", STAFF),
                Arguments.of("PUT", "/api/v1/cars/999999", STAFF),
                Arguments.of("DELETE", "/api/v1/cars/999999", ADMIN),
                Arguments.of("POST", "/api/v1/cars/999999/restore", ADMIN),

                Arguments.of("GET", "/api/v1/customers", STAFF),
                Arguments.of("POST", "/api/v1/customers", STAFF),

                Arguments.of("GET", "/api/v1/reservations", STAFF),
                Arguments.of("POST", "/api/v1/reservations", STAFF),
                Arguments.of("POST", "/api/v1/reservations/999999/confirm", STAFF),

                Arguments.of("GET", "/api/v1/maintenance", STAFF),
                Arguments.of("GET", "/api/v1/maintenance/999999", STAFF),
                Arguments.of("PUT", "/api/v1/maintenance/999999", STAFF),
                Arguments.of("POST", "/api/v1/maintenance/999999/complete", STAFF),
                Arguments.of("GET", "/api/v1/maintenance/due-cars", STAFF),

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
            req = req.with(user("test-" + principal.toLowerCase()).roles(principal));
        }

        int status = mockMvc.perform(req).andReturn().getResponse().getStatus();

        if (allowed) {
            assertThat(status).as("%s %s as %s should pass security", method, path, principal).isNotEqualTo(403);
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
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isForbidden());
    }

    @Test
    void validJwtGrantsTheUsersRoles() throws Exception {
        User admin = userRepository.findByUsernameWithRoles("admin").orElseThrow();
        String token = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
                .andExpect(status().isForbidden());
    }
}
