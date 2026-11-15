package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.entity.Branch;
import com.nextstep.rentacar.repository.BranchRepository;
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

    @Autowired
    private BranchRepository branchRepository;

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

    @Test
    @DisplayName("No default branch is set until one is chosen")
    void defaultBranchStartsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(jsonPath("$.defaultBranchId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("A default branch can be chosen, is persisted and can be cleared")
    void setsAndClearsDefaultBranch() throws Exception {
        Long branchId = newBranch().getId();

        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"KILOMETERS\",\"defaultBranchId\":" + branchId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultBranchId").value(branchId));
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(jsonPath("$.defaultBranchId").value(branchId));

        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"KILOMETERS\",\"defaultBranchId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultBranchId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("An unknown branch is 404 and an inactive one is 409")
    void rejectsUnknownAndInactiveDefaultBranch() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"KILOMETERS\",\"defaultBranchId\":999999}"))
                .andExpect(status().isNotFound());

        Branch branch = newBranch();
        branch.setActive(false);
        branchRepository.saveAndFlush(branch);
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"KILOMETERS\",\"defaultBranchId\":" + branch.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A default branch that is closed later is no longer reported")
    void closedDefaultBranchIsDropped() throws Exception {
        Branch branch = newBranch();
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileageUnit\":\"KILOMETERS\",\"defaultBranchId\":" + branch.getId() + "}"))
                .andExpect(status().isOk());

        branch.setActive(false);
        branchRepository.saveAndFlush(branch);

        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(jsonPath("$.defaultBranchId").value(org.hamcrest.Matchers.nullValue()));
    }

    private Branch newBranch() {
        Branch branch = new Branch();
        branch.setName("Settings Branch");
        branch.setAddress("1 Test St");
        branch.setCity("Test City");
        branch.setCountry("Test Country");
        branch.setPhone("+1-555-0100");
        branch.setEmail("settings@test.com");
        branch.setOpeningHours("9:00-18:00");
        return branchRepository.saveAndFlush(branch);
    }
}
