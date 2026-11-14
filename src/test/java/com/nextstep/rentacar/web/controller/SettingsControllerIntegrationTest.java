package com.nextstep.rentacar.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Mileage unit defaults to kilometers")
    void defaultsToKilometers() throws Exception {
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileageUnit").value("KILOMETERS"));
    }

    @Test
    @DisplayName("Mileage unit can be changed and is persisted")
    void updatesMileageUnit() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"MILES\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileageUnit").value("MILES"));

        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(jsonPath("$.mileageUnit").value("MILES"));
    }

    @Test
    @DisplayName("Unknown or missing unit is rejected")
    void rejectsInvalidUnit() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"FURLONGS\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
