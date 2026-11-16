package com.nextstep.rentacar.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A photo of a car. The bytes live in the configured image directory (see CarImageStorage);
 * only the metadata and the storage key are kept in the database.
 */
@Entity
@Table(name = "car_images")
@Getter
@Setter
@NoArgsConstructor
public class CarImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    /** Path relative to the image directory, e.g. "7/3f1c...-....png". Never sent to clients. */
    @Column(name = "storage_key", nullable = false, unique = true, length = 120)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Upload order within the car (0 first); gaps are fine after a delete. */
    @Column(name = "position", nullable = false)
    private int position;
}
