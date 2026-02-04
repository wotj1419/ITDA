package com.itda.backend.worker.video;

import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.job.domain.Job;
import com.itda.backend.worker.AssetRegistrar;
import com.itda.backend.worker.image.ImageStorage;
import com.itda.backend.worker.image.ImageStorageResult;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoThumbnailService {

    private static final Duration FFMPEG_TIMEOUT = Duration.ofMinutes(1);
    private static final String THUMB_CONTENT_TYPE = "image/png";
    private static final List<String> SEEK_SECONDS = List.of("0.8", "0.3", "0");
    private static final String METRIC_THUMBNAIL_FAILURE = "itda.thumbnail.failure.total";
    private static final String METRIC_THUMBNAIL_SUCCESS = "itda.thumbnail.success.total";

    private final ImageStorage imageStorage;
    private final AssetRegistrar assetRegistrar;
    private final MeterRegistry meterRegistry;

    public Optional<ThumbnailAsset> createForVideoFile(Job job, Path videoPath, String context) {
        if (!isValidJob(job)) {
            incrementFailure(job, "validate", "invalid_job");
            return Optional.empty();
        }
        if (videoPath == null || !Files.exists(videoPath)) {
            log.warn("[VideoThumbnailService] Skip thumbnail: video file missing ({})", context);
            incrementFailure(job, "validate", "video_missing");
            return Optional.empty();
        }

        Path outputPath = null;
        try {
            outputPath = Files.createTempFile("thumb-", ".png");
            if (!extractThumbnail(videoPath, outputPath, context)) {
                incrementFailure(job, "ffmpeg", "extract_failed");
                return Optional.empty();
            }
            byte[] bytes = Files.readAllBytes(outputPath);
            if (bytes.length == 0) {
                incrementFailure(job, "validate", "empty_output");
                return Optional.empty();
            }
            ThumbnailAsset asset = storeThumbnailAsset(job, bytes);
            incrementSuccess(job);
            return Optional.of(asset);
        } catch (Exception e) {
            log.warn("[VideoThumbnailService] Thumbnail generation failed ({})", context, e);
            incrementFailure(job, "store", "exception");
            return Optional.empty();
        } finally {
            deleteQuietly(outputPath);
        }
    }

    public Optional<ThumbnailAsset> createForVideoBytes(Job job, byte[] videoBytes, String context) {
        if (!isValidJob(job)) {
            incrementFailure(job, "validate", "invalid_job");
            return Optional.empty();
        }
        if (videoBytes == null || videoBytes.length == 0) {
            incrementFailure(job, "validate", "video_empty");
            return Optional.empty();
        }

        Path tempVideoPath = null;
        try {
            tempVideoPath = Files.createTempFile("video-thumb-input-", ".mp4");
            Files.write(tempVideoPath, videoBytes);
            return createForVideoFile(job, tempVideoPath, context);
        } catch (Exception e) {
            log.warn("[VideoThumbnailService] Failed to prepare thumbnail input ({})", context, e);
            incrementFailure(job, "prepare", "temp_video_failed");
            return Optional.empty();
        } finally {
            deleteQuietly(tempVideoPath);
        }
    }

    private ThumbnailAsset storeThumbnailAsset(Job job, byte[] thumbnailBytes) {
        ImageStorageResult stored = imageStorage.save(
                job.getProjectId(),
                job.getId(),
                thumbnailBytes,
                THUMB_CONTENT_TYPE
        );

        Long thumbnailAssetId = assetRegistrar.registerAsset(
                job,
                stored.storageKey(),
                stored.sizeBytes(),
                AssetType.IMAGE,
                stored.contentType(),
                stored.storageProvider()
        );
        return new ThumbnailAsset(thumbnailAssetId, stored.storageKey());
    }

    private boolean extractThumbnail(Path videoPath, Path outputPath, String context) {
        for (String seekSeconds : SEEK_SECONDS) {
            ProcessResult result = runFfmpegThumbnail(videoPath, outputPath, seekSeconds);
            if (result.exitCode() == 0 && fileHasSize(outputPath)) {
                return true;
            }
            log.debug(
                    "[VideoThumbnailService] Thumbnail extract retry: context={}, seek={}, exitCode={}",
                    context,
                    seekSeconds,
                    result.exitCode()
            );
        }
        log.warn("[VideoThumbnailService] Thumbnail extraction failed after retries ({})", context);
        return false;
    }

    private ProcessResult runFfmpegThumbnail(Path videoPath, Path outputPath, String seekSeconds) {
        List<String> command = List.of(
                "ffmpeg",
                "-y",
                "-ss",
                seekSeconds,
                "-i",
                videoPath.toString(),
                "-vframes",
                "1",
                "-vf",
                "scale=640:-1",
                outputPath.toString()
        );
        return runProcess(command, FFMPEG_TIMEOUT);
    }

    private ProcessResult runProcess(List<String> command, Duration timeout) {
        Path logFile = null;
        try {
            logFile = Files.createTempFile("thumb-ffmpeg-", ".log");
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, "timeout");
            }

            String output = Files.exists(logFile)
                    ? Files.readString(logFile, StandardCharsets.UTF_8)
                    : "";
            return new ProcessResult(process.exitValue(), output);
        } catch (Exception e) {
            return new ProcessResult(-1, e.getMessage());
        } finally {
            deleteQuietly(logFile);
        }
    }

    private boolean isValidJob(Job job) {
        return job != null && job.getProjectId() != null && job.getId() != null;
    }

    private boolean fileHasSize(Path path) {
        try {
            return path != null && Files.exists(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // ignore cleanup failure
        }
    }

    public record ThumbnailAsset(Long assetId, String storageKey) {
    }

    private record ProcessResult(int exitCode, String output) {
    }

    private void incrementFailure(Job job, String stage, String reason) {
        meterRegistry.counter(
                METRIC_THUMBNAIL_FAILURE,
                "jobType", resolveJobType(job),
                "stage", stage,
                "reason", reason
        ).increment();
    }

    private void incrementSuccess(Job job) {
        meterRegistry.counter(
                METRIC_THUMBNAIL_SUCCESS,
                "jobType", resolveJobType(job)
        ).increment();
    }

    private String resolveJobType(Job job) {
        if (job == null || job.getType() == null) {
            return "UNKNOWN";
        }
        return job.getType().name();
    }
}
