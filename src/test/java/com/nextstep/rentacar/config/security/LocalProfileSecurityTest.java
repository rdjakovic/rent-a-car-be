package com.nextstep.rentacar.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Running the app as `local` (the default profile, and what `docker` builds on) uses the real security rules:
 * only the automated tests (`test` profile) get the open chain. Uses the seeded development users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalProfileSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("The local profile asks for a sign-in; a seeded user can sign in and use the API")
    void localProfileNeedsASignIn() throws Exception {
        mockMvc.perform(get("/api/v1/cars")).andExpect(status().isUnauthorized());

        String body = objectMapper.writeValueAsString(Map.of("username", "john.doe", "password", "admin123"));
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("STAFF"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("access_token").asString();

        mockMvc.perform(get("/api/v1/cars").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        // Staff can't add cars: the rights apply here too
        mockMvc.perform(post("/api/v1/cars").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
