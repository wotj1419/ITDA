package com.itda.backend.worker.merge;

import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.node.repository.dto.TimelineNodeRow;
import com.itda.backend.worker.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg 병합 Worker (SCENE_MERGE / PROJECT_MERGE)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MergeWorker {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(10);
    private static final String EXPORTS_DIR = "exports";
    private static final String SCENE_EXPORTS_DIR = "scenes";
    private static final String EXPORT_FILE_NAME = "final.mp4";
    private static final String FILES_PREFIX = "/files/";

    private final NodeMapper nodeMapper;
    private final FileStorageProperties fileStorageProperties;

    public ExecutionResult execute(Job job) {
        if (job == null) {
            throw new IllegalStateException("Job is required");
        }
        if (job.getType() == null) {
            throw new IllegalStateException("Job missing type");
        }

        return switch (job.getType()) {
            case PROJECT_MERGE -> mergeProject(job);
            case SCENE_MERGE -> mergeScene(job);
            default -> throw new IllegalStateException("Unsupported merge type: " + job.getType());
        };
    }

    private ExecutionResult mergeProject(Job job) {
        Long projectId = job.getProjectId();
        if (projectId == null) {
            throw new IllegalStateException("Project merge job missing projectId");
        }

        List<TimelineNodeRow> rows = nodeMapper.findConfirmedVideoNodesByProjectId(projectId);
        if (rows.isEmpty()) {
            throw new IllegalStateException("No confirmed video nodes to merge");
        }

        Path outputPath = resolveProjectExportPath(projectId);
        mergeConfirmedVideos(rows, outputPath);
        return new ExecutionResult(null, resolveRelativeContentKey(outputPath));
    }

    private ExecutionResult mergeScene(Job job) {
        Long sceneId = job.getSceneId();
        if (sceneId == null) {
            throw new IllegalStateException("Scene merge job missing sceneId");
        }

        List<TimelineNodeRow> rows = nodeMapper.findConfirmedVideoNodesBySceneId(sceneId);
        if (rows.isEmpty()) {
            throw new IllegalStateException("No confirmed video nodes to merge");
        }

        Path outputPath = resolveSceneExportPath(sceneId);
        mergeConfirmedVideos(rows, outputPath);
        return new ExecutionResult(null, resolveRelativeContentKey(outputPath));
    }

    private void mergeConfirmedVideos(List<TimelineNodeRow> rows, Path outputPath) {
        List<Path> inputPaths = resolveInputPaths(rows);
        ensureParentDir(outputPath);

        Path concatList = createConcatListFile(inputPaths, outputPath.getParent());
        try {
            boolean keepAudio = allHaveAudio(inputPaths);
            runFfmpeg(concatList, outputPath, keepAudio);
        } finally {
            deleteQuietly(concatList);
        }
    }

    private List<Path> resolveInputPaths(List<TimelineNodeRow> rows) {
        List<Path> paths = new ArrayList<>();
        for (TimelineNodeRow row : rows) {
            Path path = resolveNodeVideoPath(row.getVideoNodeId(), row.getContentUrl());
            if (!Files.exists(path)) {
                throw new IllegalStateException(
                        "Video file missing: nodeId=" + row.getVideoNodeId() + ", path=" + path);
            }
            paths.add(path);
        }
        return paths;
    }

    private Path resolveNodeVideoPath(Long nodeId, String contentUrl) {
        String contentKey = normalizeContentKey(contentUrl);
        if (contentKey == null) {
            contentKey = defaultNodeContentKey(nodeId);
        }
        if (contentKey == null) {
            throw new IllegalStateException("Node content missing: nodeId=" + nodeId);
        }
        return resolveUnderUploadRoot(contentKey);
    }

    private String normalizeContentKey(String contentUrl) {
        if (contentUrl == null) {
            return null;
        }
        String trimmed = contentUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = new URI(trimmed);
                String path = uri.getPath();
                if (path == null || path.isBlank()) {
                    return null;
                }
                trimmed = path;
            } catch (URISyntaxException e) {
                log.warn("[MergeWorker] Invalid content URL: {}", trimmed);
                return null;
            }
        }
        if (trimmed.startsWith(FILES_PREFIX)) {
            return trimmed.substring(FILES_PREFIX.length());
        }
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private String defaultNodeContentKey(Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        return "ai/videos/node-" + nodeId + ".mp4";
    }

    private Path resolveUnderUploadRoot(String relativePath) {
        Path root = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Invalid content path");
        }
        return target;
    }

    private Path resolveProjectExportPath(Long projectId) {
        return Path.of(
                fileStorageProperties.getUploadDir(),
                EXPORTS_DIR,
                String.valueOf(projectId),
                EXPORT_FILE_NAME).toAbsolutePath().normalize();
    }

    private Path resolveSceneExportPath(Long sceneId) {
        return Path.of(
                fileStorageProperties.getUploadDir(),
                EXPORTS_DIR,
                SCENE_EXPORTS_DIR,
                String.valueOf(sceneId),
                EXPORT_FILE_NAME).toAbsolutePath().normalize();
    }

    private String resolveRelativeContentKey(Path absolutePath) {
        if (absolutePath == null) {
            return null;
        }
        Path root = Path.of(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        Path normalized = absolutePath.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            return null;
        }
        return root.relativize(normalized).toString().replace("\\", "/");
    }

    private void ensureParentDir(Path outputPath) {
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create export directory", e);
        }
    }

    private Path createConcatListFile(List<Path> inputPaths, Path dir) {
        try {
            Path listFile = Files.createTempFile(dir, "concat-", ".txt");
            try (BufferedWriter writer = Files.newBufferedWriter(listFile, StandardCharsets.UTF_8)) {
                for (Path path : inputPaths) {
                    // concat 파일에 기록되는 경로 로그 출력
                    String escapedPath = escapePath(path.toAbsolutePath());
                    writer.write("file '" + escapedPath + "'");
                    writer.newLine();
                }
            }
            // 디버그 로그: 생성된 리스트 파일 내용 확인
            log.info("[MergeWorker] Created concat list file: {}", listFile);
            if (log.isDebugEnabled()) {
                log.debug("[MergeWorker] Concat file content:\n{}", Files.readString(listFile));
            }
            return listFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create concat list file", e);
        }
    }

    private String escapePath(Path path) {
        String raw = path.toString().replace("\\", "/");
        return raw.replace("'", "'\\''");
    }

    private boolean allHaveAudio(List<Path> inputPaths) {
        for (Path path : inputPaths) {
            if (!hasAudio(path)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAudio(Path path) {
        List<String> command = List.of(
                "ffprobe",
                "-v", "error",
                "-select_streams", "a",
                "-show_entries", "stream=codec_type",
                "-of", "csv=p=0",
                path.toString());
        try {
            ProcessResult result = runProcess(command, Duration.ofSeconds(20));
            return !result.output().trim().isEmpty();
        } catch (Exception e) {
            log.warn("[MergeWorker] ffprobe failed, treating as no-audio: file={}", path);
            return false;
        }
    }

    private void runFfmpeg(Path listFile, Path outputPath, boolean keepAudio) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-f");
        command.add("concat");
        command.add("-safe");
        command.add("0");
        command.add("-i");
        command.add(listFile.toString());
        command.add("-vf");
        command.add("scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast");
        command.add("-pix_fmt");
        command.add("yuv420p");
        if (keepAudio) {
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("128k");
        } else {
            command.add("-an"); // Audio 비활성화
        }
        command.add("-movflags");
        command.add("+faststart");
        command.add(outputPath.toString());

        log.info("[MergeWorker] FFmpeg command: {}", String.join(" ", command));

        ProcessResult result = runProcess(command, PROCESS_TIMEOUT);

        // 디버깅을 위한 출력 로그 (FFmpeg 실행 결과 확인)
        if (!result.output().isBlank()) {
            log.debug("[MergeWorker] FFmpeg output:\n{}", result.output());
        }

        if (result.exitCode() != 0) {
            List<String> lines = result.output().lines().toList();
            int start = Math.max(0, lines.size() - 20); // 더 많은 라인 표시 (최근 20줄)
            String summary = String.join("\n", lines.subList(start, lines.size()));
            log.error("[MergeWorker] FFmpeg failed with exitCode={}. Summary:\n{}", result.exitCode(), summary);
            throw new IllegalStateException("FFmpeg merge failed: " + summary);
        } else {
            log.info("[MergeWorker] FFmpeg merge success. Output: {}", outputPath);
        }
    }

    private ProcessResult runProcess(List<String> command, Duration timeout) {
        Path logFile = null;
        try {
            // Deadlock 방지: 출력 스트림이 꽉 차면 프로세스가 멈추므로 파일로 리다이렉트
            logFile = Files.createTempFile("ffmpeg-output-", ".log");
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();

            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Process timeout: " + String.join(" ", command));
            }

            String output = Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";
            return new ProcessResult(process.exitValue(), output);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Process interrupted: " + String.join(" ", command), e);
        } catch (IOException e) {
            throw new IllegalStateException("Process failed: " + String.join(" ", command), e);
        } finally {
            if (logFile != null) {
                deleteQuietly(logFile);
            }
        }
    }

    // private String readAll(InputStream stream) removed as it is no longer used

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // ignore
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
