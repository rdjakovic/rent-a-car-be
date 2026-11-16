package com.nextstep.rentacar.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Keeps car photos on disk. Keys are generated here (car id / UUID + extension), never taken from a client,
 * so a key can't point outside the image directory.
 */
@Component
public class CarImageStorage {

    private final Path root;

    public CarImageStorage(@Value("${app.car-images.dir:./data/car-images}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    /** Stores the bytes and returns the key to keep in the database. */
    public String store(Long carId, String extension, byte[] bytes) {
        Path carDirectory = root.resolve(String.valueOf(carId)).normalize();
        if (!carDirectory.startsWith(root)) {
            throw new IllegalArgumentException("Invalid car id for an image");
        }
        String key = carId + "/" + UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(carDirectory);
            Files.write(root.resolve(key), bytes, StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the image", e);
        }
    }

    public Resource load(String key) {
        Path path = pathOf(key);
        if (!Files.isReadable(path)) {
            throw new EntityNotFoundException("Image file not found");
        }
        return new FileSystemResource(path);
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(pathOf(key));
        } catch (IOException ignored) {
            // A missing file is what the caller wanted; anything else can't be fixed here.
        }
    }

    private Path pathOf(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new EntityNotFoundException("Image file not found");
        }
        return path;
    }
}
