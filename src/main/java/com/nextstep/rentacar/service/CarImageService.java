package com.nextstep.rentacar.service;

import com.nextstep.rentacar.dto.response.CarImageResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CarImageService {

    /** At most 5 images per car, 5 MB each, JPEG/PNG/WebP only. */
    List<CarImageResponseDto> addImages(Long carId, List<MultipartFile> files);

    ImageContent getImage(Long carId, Long imageId);

    void deleteImage(Long carId, Long imageId);

    record ImageContent(Resource resource, String contentType) {
    }
}
