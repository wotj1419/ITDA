package com.itda.backend.media;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * 미디어 파일 응답 정보
 */
public record MediaFile(
        Resource resource,
        MediaType mediaType,
        long contentLength,
        String filename
) {
}
