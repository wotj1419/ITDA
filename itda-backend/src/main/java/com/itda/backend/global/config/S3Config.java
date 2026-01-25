package com.itda.backend.global.config;

import com.itda.backend.asset.config.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
public class S3Config {

    private final S3StorageProperties properties;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(resolveRegion())
                .credentialsProvider(resolveCredentials())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyle())
                        .build());

        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        Builder builder = S3Presigner.builder()
                .region(resolveRegion())
                .credentialsProvider(resolveCredentials())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyle())
                        .build());

        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
        }

        return builder.build();
    }

    private AwsCredentialsProvider resolveCredentials() {
        if (hasText(properties.getAccessKey()) && hasText(properties.getSecretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            properties.getAccessKey().trim(),
                            properties.getSecretKey().trim()
                    )
            );
        }
        return DefaultCredentialsProvider.create();
    }

    private Region resolveRegion() {
        if (!hasText(properties.getRegion())) {
            throw new IllegalStateException("storage.s3.region must be configured");
        }
        return Region.of(properties.getRegion().trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
