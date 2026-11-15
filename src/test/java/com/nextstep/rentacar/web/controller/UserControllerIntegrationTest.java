package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.config.security.RolePermissions;
import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.domain.entity.Role;
import com.nextstep.rentacar.domain.entity.User;
import com.nextstep.rentacar.repository.BranchRepository;
import com.nextstep.rentacar.repository.RoleRepository;
import com.nextstep.rentacar.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The user API. Who may call which endpoint is covered by SecurityRulesTest; this checks what the
 * endpoints do (the test profile's open chain lets every request in, the signed-in user is set per request).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

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
    private BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Branch branch;
    private User admin;
    private RequestPostProcessor asAdmin;
    private RequestPostProcessor asManager;

    @BeforeEach
    void setUp() {
        // The test profile's schema comes from Hibernate, without Flyway's seeded roles
        for (String name : List.of("STAFF", "MANAGER", "ADMIN")) {
            roleRepository.findByName(name).orElseGet(() -> {
                Role role = new Role();
                role.setName(name);
                return roleRepository.saveAndFlush(role);
            });
        }
        branch = new Branch();
        branch.setName("Users Branch");
        branch.setAddress("1 Test St");
        branch.setCity("Test City");
        branch.setCountry("Test Country");
        branch = branchRepository.saveAndFlush(branch);

        admin = saveUser("admin-" + suffix(), "ADMIN");
        asAdmin = user(admin.getUsername()).authorities(RolePermissions.authoritiesOf(List.of("ADMIN")));
        asManager = user("manager-" + suffix()).authorities(RolePermissions.authoritiesOf(List.of("MANAGER")));
    }

    @Test
    @DisplayName("An admin creates a staff member at a branch; they can sign in and get an employee record")
    void adminCreatesStaff() throws Exception {
        String username = "new-" + suffix();
        mockMvc.perform(create(asAdmin, newUser(username, "STAFF", branch.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(username + "@test.com"))
                .andExpect(jsonPath("$.roles[0]").value("STAFF"))
                .andExpect(jsonPath("$.branchName").value("Users Branch"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.permissions").value(hasItem("RESERVATION_MANAGE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("CAR_MANAGE"))));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.branchName").value("Users Branch"));
    }

    @Test
    @DisplayName("An admin can create managers and administrators; an administrator needs no branch")
    void adminCreatesManagerAndAdmin() throws Exception {
        mockMvc.perform(create(asAdmin, newUser("mgr-" + suffix(), "MANAGER", branch.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"))
                .andExpect(jsonPath("$.permissions").value(hasItem("CAR_MANAGE")));
        mockMvc.perform(create(asAdmin, newUser("adm-" + suffix(), "ADMIN", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.permissions").value(hasItem("USER_MANAGE")));
    }

    @Test
    @DisplayName("A manager can create STAFF but not managers or administrators")
    void managerCreatesOnlyStaff() throws Exception {
        mockMvc.perform(create(asManager, newUser("stf-" + suffix(), "STAFF", branch.getId())))
                .andExpect(status().isCreated());
        mockMvc.perform(create(asManager, newUser("mgr-" + suffix(), "MANAGER", branch.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(create(asManager, newUser("adm-" + suffix(), "ADMIN", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid data: no branch for staff, a username with @, a short password, unknown branch")
    void createValidation() throws Exception {
        mockMvc.perform(create(asAdmin, newUser("nb-" + suffix(), "STAFF", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Branch is required for staff and managers"));
        mockMvc.perform(create(asAdmin, newUser("a@b", "STAFF", branch.getId())))
                .andExpect(status().isBadRequest());
        Map<String, Object> shortPassword = newUser("sp-" + suffix(), "STAFF", branch.getId());
        shortPassword.put("password", "short");
        mockMvc.perform(create(asAdmin, shortPassword)).andExpect(status().isBadRequest());
        mockMvc.perform(create(asAdmin, newUser("ub-" + suffix(), "STAFF", 999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A taken username or email is 409")
    void duplicates() throws Exception {
        String username = "dup-" + suffix();
        mockMvc.perform(create(asAdmin, newUser(username, "STAFF", branch.getId()))).andExpect(status().isCreated());

        mockMvc.perform(create(asAdmin, newUser(username, "STAFF", branch.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Username is already taken"));
        Map<String, Object> sameEmail = newUser("other-" + suffix(), "STAFF", branch.getId());
        sameEmail.put("email", username.toUpperCase() + "@TEST.COM");
        mockMvc.perform(create(asAdmin, sameEmail))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already in use"));
    }

    @Test
    @DisplayName("List with search and role filter; get by id; unknown id is 404")
    void listAndGet() throws Exception {
        String tag = "zq" + suffix();
        saveUser(tag + "-s", "STAFF");
        saveUser(tag + "-m", "MANAGER");

        mockMvc.perform(get("/api/v1/users").param("search", tag.toUpperCase()).with(asManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
        mockMvc.perform(get("/api/v1/users").param("search", tag).param("role", "MANAGER").with(asManager))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value(tag + "-m"));

        User found = userRepository.findByUsername(tag + "-s").orElseThrow();
        mockMvc.perform(get("/api/v1/users/" + found.getId()).with(asManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(tag + "-s"));
        mockMvc.perform(get("/api/v1/users/999999").with(asManager)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update changes role, branch and details; a promoted staff member gets the new rights")
    void update() throws Exception {
        User target = saveUser("upd-" + suffix(), "STAFF");
        Map<String, Object> body = updateBody(target, "MANAGER", branch.getId(), true);
        body.put("phone", "+1-555-0999");

        mockMvc.perform(put("/api/v1/users/" + target.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"))
                .andExpect(jsonPath("$.phone").value("+1-555-0999"))
                .andExpect(jsonPath("$.branchName").value("Users Branch"))
                .andExpect(jsonPath("$.permissions").value(hasItem("CAR_MANAGE")));

        Map<String, Object> noBranch = updateBody(target, "STAFF", null, true);
        mockMvc.perform(put("/api/v1/users/" + target.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(noBranch)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Disabling a user ends their sessions at once and stops sign-in")
    void disableEndsSessions() throws Exception {
        User target = saveUser("dis-" + suffix(), "STAFF");
        MvcResult signedIn = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", target.getUsername(), "password", PASSWORD)))).andReturn();
        Cookie cookie = signedIn.getResponse().getCookie(RefreshCookies.NAME);
        assertThat(cookie).isNotNull();

        mockMvc.perform(put("/api/v1/users/" + target.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody(target, "STAFF", branch.getId(), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(cookie.getName(), cookie.getValue())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", target.getUsername(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An admin can't disable or demote their own account; another admin can be changed")
    void selfGuard() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + admin.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody(admin, "ADMIN", null, false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("You can't disable or demote your own account"));
        mockMvc.perform(put("/api/v1/users/" + admin.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody(admin, "STAFF", branch.getId(), true))))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/users/" + admin.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody(admin, "ADMIN", null, true))))
                .andExpect(status().isOk());

        User other = saveUser("adm2-" + suffix(), "ADMIN");
        mockMvc.perform(put("/api/v1/users/" + other.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody(other, "STAFF", branch.getId(), true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A taken email on update is 409; keeping one's own email is fine")
    void updateEmail() throws Exception {
        User first = saveUser("em1-" + suffix(), "STAFF");
        User second = saveUser("em2-" + suffix(), "STAFF");

        Map<String, Object> taken = updateBody(second, "STAFF", branch.getId(), true);
        taken.put("email", first.getEmail());
        mockMvc.perform(put("/api/v1/users/" + second.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(taken)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/users/" + second.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody(second, "STAFF", branch.getId(), true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reset password: the old one stops working, sessions end and a sign-in lock is lifted")
    void resetPassword() throws Exception {
        User target = saveUser("rst-" + suffix(), "STAFF");
        String username = target.getUsername();
        MvcResult signedIn = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", PASSWORD)))).andReturn();
        Cookie cookie = signedIn.getResponse().getCookie(RefreshCookies.NAME);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("username", username, "password", "wrong-password"))));
        }
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(post("/api/v1/users/" + target.getId() + "/reset-password").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword", "brand-new-pass"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(cookie.getName(), cookie.getValue())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "brand-new-pass"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/" + target.getId() + "/reset-password").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword", "short"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/999999/reset-password").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword", "brand-new-pass"))))
                .andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder createBuilder() {
        return post("/api/v1/users").contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder create(RequestPostProcessor caller, Map<String, Object> body) {
        return createBuilder().with(caller).content(json(body));
    }

    private static Map<String, Object> newUser(String username, String role, Long branchId) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", username + "@test.com");
        body.put("password", PASSWORD);
        body.put("firstName", "New");
        body.put("lastName", "Person");
        body.put("role", role);
        body.put("branchId", branchId);
        return body;
    }

    private static Map<String, Object> updateBody(User user, String role, Long branchId, boolean enabled) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", user.getEmail());
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());
        body.put("role", role);
        body.put("branchId", branchId);
        body.put("enabled", enabled);
        return body;
    }

    private User saveUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Seed");
        user.setLastName("User");
        user.setEnabled(true);
        user.setRoles(Set.of(roleRepository.findByName(role).orElseThrow()));
        return userRepository.saveAndFlush(user);
    }

    private String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
