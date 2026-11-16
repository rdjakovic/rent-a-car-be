package com.nextstep.rentacar.dto.response;

/**
 * A car photo. `url` is relative to the API host (e.g. /api/v1/cars/7/images/2); the UI resolves it
 * against the configured API base.
 */
public record CarImageResponseDto(
        Long id,
        String url,
        String fileName,
        String contentType,
        long sizeBytes,
        int position
) {
}
