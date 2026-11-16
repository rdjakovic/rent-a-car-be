package com.nextstep.rentacar.service.impl;

import com.nextstep.rentacar.domain.entity.Car;
import com.nextstep.rentacar.domain.entity.CarImage;
import com.nextstep.rentacar.dto.response.CarImageResponseDto;
import com.nextstep.rentacar.repository.CarImageRepository;
import com.nextstep.rentacar.repository.CarRepository;
import com.nextstep.rentacar.service.CarImageService;
import com.nextstep.rentacar.service.CarImageStorage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarImageServiceImpl implements CarImageService {

    public static final int MAX_IMAGES = 5;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private final CarRepository carRepository;
    private final CarImageRepository imageRepository;
    private final CarImageStorage storage;

    @Override
    @Transactional
    public List<CarImageResponseDto> addImages(Long carId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one image");
        }
        // Lock the car so two uploads can't both take the last free slot
        Car car = carRepository.findByIdForUpdate(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));

        long existing = imageRepository.countByCarId(carId);
        if (existing + files.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("A car can have at most " + MAX_IMAGES + " images");
        }
        List<ValidatedImage> validated = files.stream().map(this::validate).toList();

        int position = imageRepository.findMaxPosition(carId) + 1;
        List<String> storedKeys = new ArrayList<>();
        try {
            List<CarImage> saved = new ArrayList<>();
            for (ValidatedImage image : validated) {
                String key = storage.store(carId, image.extension(), image.bytes());
                storedKeys.add(key);

                CarImage entity = new CarImage();
                entity.setCar(car);
                entity.setStorageKey(key);
                entity.setFileName(image.fileName());
                entity.setContentType(image.contentType());
                entity.setSizeBytes(image.bytes().length);
                entity.setPosition(position++);
                saved.add(imageRepository.save(entity));
                // Keep the inverse side in sync for the same persistence context (e.g. a create followed by a read)
                car.getImages().add(entity);
            }
            return saved.stream().map(this::toDto).toList();
        } catch (RuntimeException e) {
            // Don't leave files behind for rows the transaction will roll back
            storedKeys.forEach(storage::delete);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ImageContent getImage(Long carId, Long imageId) {
        CarImage image = imageRepository.findByIdAndCarId(imageId, carId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
        return new ImageContent(storage.load(image.getStorageKey()), image.getContentType());
    }

    @Override
    @Transactional
    public void deleteImage(Long carId, Long imageId) {
        Car car = carRepository.findByIdForUpdate(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));
        CarImage image = imageRepository.findByIdAndCarId(imageId, carId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
        // orphanRemoval deletes the row; keeping the collection in sync matters within one persistence context
        car.getImages().remove(image);
        imageRepository.flush();
        storage.delete(image.getStorageKey());
    }

    private CarImageResponseDto toDto(CarImage image) {
        Long carId = image.getCar().getId();
        return new CarImageResponseDto(
                image.getId(),
                "/api/v1/cars/" + carId + "/images/" + image.getId(),
                image.getFileName(),
                image.getContentType(),
                image.getSizeBytes(),
                image.getPosition());
    }

    private ValidatedImage validate(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        if (file.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Each image must be 5 MB or smaller");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read " + name, e);
        }
        DetectedImage detected = detect(bytes);
        return new ValidatedImage(bytes, detected.contentType(), detected.extension(), cleanName(name, detected.extension()));
    }

    /** The content type comes from the bytes, not from what the client claims. */
    private DetectedImage detect(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return new DetectedImage("image/jpeg", "jpg");
        }
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return new DetectedImage("image/png", "png");
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new DetectedImage("image/webp", "webp");
        }
        throw new IllegalArgumentException("Images must be JPEG, PNG or WebP");
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String cleanName(String original, String extension) {
        String base = original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (base.isEmpty()) {
            base = "image." + extension;
        }
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private record ValidatedImage(byte[] bytes, String contentType, String extension, String fileName) {
    }

    private record DetectedImage(String contentType, String extension) {
    }
}
