package com.nextstep.rentacar.web.controller;

import com.nextstep.rentacar.dto.response.CarImageResponseDto;
import com.nextstep.rentacar.service.CarImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cars")
public class CarImageController {

    private final CarImageService carImageService;

    public CarImageController(CarImageService carImageService) {
        this.carImageService = carImageService;
    }

    @PostMapping(value = "/{carId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add photos to a car",
            description = "Up to 5 images per car, 5 MB each. JPEG, PNG and WebP only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uploaded", content = @Content(schema = @Schema(implementation = CarImageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Wrong type, too large, or too many images", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Car not found", content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<List<CarImageResponseDto>> addImages(
            @PathVariable Long carId,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(carImageService.addImages(carId, files));
    }

    @GetMapping("/{carId}/images/{imageId}")
    @Operation(summary = "Get a car photo",
            description = "Public so a plain <img> tag can load it; photos are not sensitive.")
    public ResponseEntity<Resource> getImage(@PathVariable Long carId, @PathVariable Long imageId) {
        CarImageService.ImageContent image = carImageService.getImage(carId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(image.resource());
    }

    @DeleteMapping("/{carId}/images/{imageId}")
    @Operation(summary = "Delete a car photo")
    public ResponseEntity<Void> deleteImage(@PathVariable Long carId, @PathVariable Long imageId) {
        carImageService.deleteImage(carId, imageId);
        return ResponseEntity.noContent().build();
    }
}
