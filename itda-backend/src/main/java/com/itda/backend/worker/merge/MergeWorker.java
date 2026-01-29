package com.itda.backend.worker.merge;

import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.video.VideoStorage;
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
import java.util.Optional;

/**
 * FFmpeg 병합 Worker (SCENE_MERGE / PROJECT_MERGE)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MergeWorker {

    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(10);
    private static final String PROJECTS_DIR = "projects";
    private static final String SCENES_DIR = "scenes";
    private static final String MERGES_DIR = "merges";
    private static final String TEMP_MERGE_DIR = "tmp/merges";
    private static final String MERGE_EXTENSION = ".mp4";
    private static final String FILES_PREFIX = "/files/";

    private final TimelineMapper timelineMapper;
    private final AssetMapper assetMapper;
    private final VideoStorage videoStorage;
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

        List<ProjectTimelineItem> items = timelineMapper.findProjectTimelineItems(projectId);
        if (items.isEmpty()) {
            throw new IllegalStateException("No project timeline items to merge");
        }

        String mergeSignature = requireMergeSignature(job);
        String storageKey = buildProjectMergeStorageKey(projectId, mergeSignature);
        Path outputPath = createTempOutputPath(projectId);
        try {
            mergeTimelineItems(resolveProjectInputPaths(items), outputPath);
            Asset asset = videoStorage.storeMergedVideo(outputPath, storageKey);
            return new ExecutionResult(asset.getId(), asset.getStorageKey());
        } finally {
            deleteQuietly(outputPath);
        }
    }

    private ExecutionResult mergeScene(Job job) {
        Long sceneId = job.getSceneId();
        if (sceneId == null) {
            throw new IllegalStateException("Scene merge job missing sceneId");
        }

        List<SceneTimelineItem> items = timelineMapper.findSceneTimelineItems(sceneId);
        if (items.isEmpty()) {
            throw new IllegalStateException("No scene timeline items to merge");
        }

        String mergeSignature = requireMergeSignature(job);
        String storageKey = buildSceneMergeStorageKey(job.getProjectId(), sceneId, mergeSignature);
        Path outputPath = createTempOutputPath(job.getProjectId());
        try {
            mergeTimelineItems(resolveSceneInputPaths(items), outputPath);
            Asset asset = videoStorage.storeMergedVideo(outputPath, storageKey);
            return new ExecutionResult(asset.getId(), asset.getStorageKey());
        } finally {
            deleteQuietly(outputPath);
        }
    }

    private void mergeTimelineItems(List<Path> inputPaths, Path outputPath) {
        ensureParentDir(outputPath);

        Path concatList = createConcatListFile(inputPaths, outputPath.getParent());
        try {
            boolean keepAudio = allHaveAudio(inputPaths);
            runFfmpeg(concatList, outputPath, keepAudio);
        } finally {
            deleteQuietly(concatList);
        }
    }

    private List<Path> resolveSceneInputPaths(List<SceneTimelineItem> items) {
        List<Path> paths = new ArrayList<>();
        for (SceneTimelineItem item : items) {
            String context = "sceneId=" + item.getSceneId() + ", videoNodeId=" + item.getVideoNodeId();
            Path path = resolveVideoPath(item.getAssetId(), item.getFallbackUrl(), context);
            paths.add(path);
        }
        return paths;
    }

    private List<Path> resolveProjectInputPaths(List<ProjectTimelineItem> items) {
        List<Path> paths = new ArrayList<>();
        for (ProjectTimelineItem item : items) {
            String context = "sceneId=" + item.getSceneId() + ", sceneVideoId=" + item.getSceneVideoId();
            Path path = resolveVideoPath(item.getAssetId(), null, context);
            paths.add(path);
        }
        return paths;
    }

    private Path resolveVideoPath(Long assetId, String fallbackUrl, String context) {
        String contentKey = resolveAssetStorageKey(assetId);
        if (contentKey == null) {
            contentKey = normalizeContentKey(fallbackUrl);
        }
        if (contentKey == null) {
            throw new IllegalStateException("Video content missing: " + context);
        }
        Path path = resolveUnderUploadRoot(contentKey);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Video file missing: " + context + ", path=" + path);
        }
        return path;
    }

    private String resolveAssetStorageKey(Long assetId) {
        if (assetId == null) {
            return null;
        }
        Optional<Asset> asset = assetMapper.findById(assetId);
        if (asset.isEmpty()) {
            throw new IllegalStateException("Asset not found: assetId=" + assetId);
        }
        String key = asset.get().getStorageKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Asset storageKey missing: assetId=" + assetId);
        }
        return key;
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

    private String requireMergeSignature(Job job) {
        String mergeSignature = job.getMergeSignature();
        if (mergeSignature == null || mergeSignature.isBlank()) {
            throw new IllegalStateException("Merge job missing mergeSignature");
        }
        return mergeSignature.trim();
    }

    private String buildProjectMergeStorageKey(Long projectId, String mergeSignature) {
        return String.join("/",
                PROJECTS_DIR,
                String.valueOf(projectId),
                MERGES_DIR,
                mergeSignature + MERGE_EXTENSION
        );
    }

    private String buildSceneMergeStorageKey(Long projectId, Long sceneId, String mergeSignature) {
        if (projectId == null) {
            throw new IllegalStateException("Scene merge job missing projectId");
        }
        return String.join("/",
                PROJECTS_DIR,
                String.valueOf(projectId),
                SCENES_DIR,
                String.valueOf(sceneId),
                MERGES_DIR,
                mergeSignature + MERGE_EXTENSION
        );
    }

    private Path createTempOutputPath(Long projectId) {
        try {
            String projectDir = projectId == null ? "unknown" : String.valueOf(projectId);
            Path tempDir = Path.of(
                    fileStorageProperties.getUploadDir(),
                    TEMP_MERGE_DIR,
                    projectDir
            ).toAbsolutePath().normalize();
            Files.createDirectories(tempDir);
            return Files.createTempFile(tempDir, "merge-", MERGE_EXTENSION);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create merge output temp file", e);
        }
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
