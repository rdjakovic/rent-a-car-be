package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.domain.enums.MaintenanceType;
import com.nextstep.rentacar.dto.request.MaintenanceScheduleRequestDto;
import com.nextstep.rentacar.service.BranchService;
import com.nextstep.rentacar.service.CarService;
import com.nextstep.rentacar.service.MaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.nextstep.rentacar.testutil.builders.BranchTestDataBuilder.aBranch;
import static com.nextstep.rentacar.testutil.builders.CarTestDataBuilder.aCar;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MaintenanceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MaintenanceService maintenanceService;
    @Autowired private BranchService branchService;
    @Autowired private CarService carService;

    private Long maintenanceId;

    @BeforeEach
    void setUp() {
        Long branchId = branchService.create(aBranch().inNewYork().build()).getId();
        Long carId = carService.create(aCar().withBranchId(branchId).build()).getId();

        MaintenanceScheduleRequestDto request = new MaintenanceScheduleRequestDto();
        request.setCarId(carId);
        request.setType(MaintenanceType.ROUTINE);
        request.setDescription("Oil change");
        request.setScheduledDate(LocalDate.now());
        maintenanceId = maintenanceService.schedule(request).getId();
        maintenanceService.start(maintenanceId);
    }

    @Test
    @DisplayName("GET /{id} returns the job; unknown id is 404")
    void getById() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance/{id}", maintenanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.car.id").isNumber());
        mockMvc.perform(get("/api/v1/maintenance/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Complete works without a body")
    void completeWithoutBody() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance/{id}/complete", maintenanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Complete records the details sent in the body")
    void completeWithBody() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance/{id}/complete", maintenanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cost\": 99.90, \"odometerReading\": 999999, \"notes\": \"Done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cost").value(99.90))
                .andExpect(jsonPath("$.odometerReading").value(999999))
                .andExpect(jsonPath("$.notes").value("Done"));
    }

    @Test
    @DisplayName("Invalid completion details and due-cars range are rejected with 400")
    void validation() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance/{id}/complete", maintenanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cost\": -1, \"odometerReading\": -5}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/maintenance/due-cars").param("withinDays", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/maintenance/due-cars"))
                .andExpect(status().isOk());
    }
}
