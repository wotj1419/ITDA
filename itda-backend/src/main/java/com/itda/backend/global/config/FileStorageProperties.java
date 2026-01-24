package com.itda.backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 파일 저장 경로 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    /**
     * 업로드 루트 경로
     */
    private String uploadDir = "./uploads";
}
