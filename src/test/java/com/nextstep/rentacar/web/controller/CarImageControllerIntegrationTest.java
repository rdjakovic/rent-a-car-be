package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.service.BranchService;
import com.nextstep.rentacar.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static com.nextstep.rentacar.testutil.builders.BranchTestDataBuilder.aBranch;
import static com.nextstep.rentacar.testutil.builders.CarTestDataBuilder.aCar;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CarImageControllerIntegrationTest {

    /** A valid 1x1 PNG, so the storage writes and serves real image bytes. */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=");

    private static final Path IMAGE_DIR = tempDirectory();

    @DynamicPropertySource
    static void imageDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.car-images.dir", IMAGE_DIR::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarService carService;

    @Autowired
    private BranchService branchService;

    private Long carId;

    @BeforeEach
    void setUp() {
        Long branchId = branchService.create(aBranch().inNewYork().build()).getId();
        carId = carService.create(aCar().withBranchId(branchId).build()).getId();
    }

    @Test
    @DisplayName("POST /api/v1/cars/{id}/images - stores up to five photos and the car lists them")
    void uploadsFiveImagesAndListsThem() throws Exception {
        mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId)
                        .file(png("one.png"))
                        .file(png("two.png"))
                        .file(png("three.png"))
                        .file(png("four.png"))
                        .file(png("five.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].url", startsWith("/api/v1/cars/" + carId + "/images/")))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[4].position").value(4));

        mockMvc.perform(get("/api/v1/cars/{id}", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(5)))
                .andExpect(jsonPath("$.images[0].fileName").value("one.png"));
    }

    @Test
    @DisplayName("A sixth photo is refused")
    void refusesMoreThanFive() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId).file(png(i + ".png")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId).file(png("six.png")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A car can have at most 5 images"));
    }

    @Test
    @DisplayName("Files over 5 MB and non-images are refused")
    void refusesOversizedAndUnsupportedImages() throws Exception {
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, tooLarge, 0, Math.min(PNG.length, tooLarge.length));

        mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId)
                        .file(new MockMultipartFile("files", "big.png", "image/png", tooLarge)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Each image must be 5 MB or smaller"));

        mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId)
                        .file(new MockMultipartFile("files", "notes.txt", "text/plain", "hello".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Images must be JPEG, PNG or WebP"));

        mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId)
                        .file(new MockMultipartFile("files", "empty.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("empty.png is empty"));
    }

    @Test
    @DisplayName("GET /api/v1/cars/{carId}/images/{imageId} serves the stored bytes")
    void servesTheImageBytes() throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId).file(png("car.png")))
                .andExpect(status().isOk())
                .andReturn();
        long imageId = objectMapper.readTree(upload.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(get("/api/v1/cars/{carId}/images/{imageId}", carId, imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(get("/api/v1/cars/{carId}/images/{imageId}", carId, 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE removes the photo and its file")
    void deletesAnImage() throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/api/v1/cars/{id}/images", carId)
                        .file(png("first.png"))
                        .file(png("second.png")))
                .andExpect(status().isOk())
                .andReturn();
        long firstId = objectMapper.readTree(upload.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(delete("/api/v1/cars/{carId}/images/{imageId}", carId, firstId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cars/{id}", carId))
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.images[0].fileName").value("second.png"));
        mockMvc.perform(get("/api/v1/cars/{carId}/images/{imageId}", carId, firstId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Photos of an unknown car are a 404")
    void unknownCarIsNotFound() throws Exception {
        mockMvc.perform(multipart("/api/v1/cars/{id}/images", 999_999L).file(png("car.png")))
                .andExpect(status().isNotFound());
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", PNG);
    }

    private static Path tempDirectory() {
        try {
            return Files.createTempDirectory("rentacar-car-images");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
