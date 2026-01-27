package com.itda.backend.ai.prompt;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.node.domain.NodeType;
import com.itda.backend.scene.domain.Scene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PromptRenderer {

    private static final String DEFAULT_NONE = "None";

    private final KoEnTranslator koEnTranslator;
    private final VideoActionPlanGenerator videoActionPlanGenerator;

    public String render(NodeType nodeType, Scene scene, String promptKo, Map<String, Object> settings) {
        if (nodeType == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String promptKoTrimmed = Optional.ofNullable(promptKo).map(String::trim).orElse("");
        if (promptKoTrimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty");
        }

        return switch (nodeType) {
            case MASTER -> renderMaster(scene, promptKoTrimmed, settings);
            case GRID -> {
                requireGridShotVariations(settings);
                yield renderGridShotVariations(scene, promptKoTrimmed, settings);
            }
            case SHOT -> renderShot(scene, promptKoTrimmed, settings);
            case VIDEO -> renderVideo(scene, promptKoTrimmed, settings);
            case SCENE_HEADER -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
    }

    private void requireGridShotVariations(Map<String, Object> settings) {
        String gridMode = readString(settings, "gridMode");
        if (!gridMode.isEmpty() && !gridMode.equalsIgnoreCase("SHOT_VARIATIONS")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "GRID gridMode not supported in P0");
        }
    }

    private String renderMaster(Scene scene, String promptKo, Map<String, Object> settings) {
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(Map.of(
                "sceneTitle", scene == null ? "" : safe(scene.getTitle()),
                "sceneDescription", scene == null ? "" : safe(scene.getDescription()),
                "promptKo", promptKo
        ));
        String sceneTitleEn = translated.getOrDefault("sceneTitle", "");
        String sceneDescriptionEn = translated.getOrDefault("sceneDescription", "");
        String promptKoEn = translated.getOrDefault("promptKo", "");

        String aspectRatio = readString(settings, "aspectRatio");
        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        List<String> lines = new ArrayList<>();
        lines.add("Scene: " + safeOrNone(sceneTitleEn) + ". " + safeOrNone(sceneDescriptionEn) + ".");
        lines.add("Content: " + requireEn(promptKoEn) + ".");
        lines.add("Wide establishing shot, single still frame.");
        lines.add("Style: " + safeOrNone(style) + ". Time: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
        lines.add("Keep character identity, outfits, lighting, and key props consistent.");
        lines.add("No text, no subtitles, no watermark, no logo.");
        lines.add("Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderGridShotVariations(Scene scene, String promptKo, Map<String, Object> settings) {
        String layout = readString(settings, "layout");
        String aspectRatio = readString(settings, "aspectRatio");
        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("promptKo", promptKo);
        translationInput.put("compositionHintKo", readString(settings, "compositionHintKo"));
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(translationInput);
        String promptKoEn = translated.getOrDefault("promptKo", "");
        String compositionHintEn = translated.getOrDefault("compositionHintKo", "");

        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        List<String> shotTypes = readStringList(settings, "shotTypes");
        List<String> lines = new ArrayList<>();
        lines.add("Create ONE storyboard grid image with " + safeOrNone(layout) + " panels, all showing the SAME moment in the SAME scene (not sequential).");
        lines.add("Base content: " + requireEn(promptKoEn) + ".");
        lines.add("Panels must differ only by camera framing:");
        for (int i = 0; i < shotTypes.size(); i++) {
            String shotTypeEn = PresetFragments.shotTypeEn(shotTypes.get(i));
            lines.add("Panel " + (i + 1) + ": " + safeOrNone(shotTypeEn));
        }
        lines.add("All panels share consistent characters, outfits, lighting, and location.");
        lines.add("Style: " + safeOrNone(style) + ". Time: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
        lines.add("Composition note: " + safeOrNone(compositionHintEn) + ".");
        lines.add("No captions, no text, no watermark, no logo. Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderShot(Scene scene, String promptKo, Map<String, Object> settings) {
        String aspectRatio = readString(settings, "aspectRatio");
        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("promptKo", promptKo);
        translationInput.put("detailKo", readString(settings, "detailKo"));
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(translationInput);
        String promptKoEn = translated.getOrDefault("promptKo", "");
        String detailEn = translated.getOrDefault("detailKo", "");
        Integer gridCellIndex = readInt(settings, "gridCellIndex");

        String shotTypeEn = PresetFragments.shotTypeEn(read(settings, "shotType"));
        String expressionEn = PresetFragments.expressionEn(read(settings, "expressionKey"));

        List<String> lines = new ArrayList<>();
        lines.add("High-quality single cinematic frame based on storyboard cell #" + (gridCellIndex == null ? 0 : gridCellIndex) + ".");
        lines.add("Content: " + requireEn(promptKoEn) + ".");
        lines.add("Camera framing: " + safeOrNone(shotTypeEn) + ". Facial expression: " + safeOrNone(expressionEn) + ".");
        lines.add("Extra detail: " + safeOrNone(detailEn) + ".");
        lines.add("Match the master look and character identity. No text, no watermark, no logo.");
        lines.add("Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderVideo(Scene scene, String promptKo, Map<String, Object> settings) {
        Integer duration = readInt(settings, "duration");
        String cameraMotionEn = resolveCameraMotionEn(settings);
        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("promptKo", promptKo);
        String motionDescriptionRaw = readString(settings, "motionDescriptionKo");
        if (motionDescriptionRaw.isBlank()) {
            motionDescriptionRaw = readString(settings, "motionDescription");
        }
        translationInput.put("motionDescription", motionDescriptionRaw);
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(translationInput);
        String promptKoEn = translated.getOrDefault("promptKo", "");
        String motionDescriptionEn = translated.getOrDefault("motionDescription", "");

        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        String actionPlanEn = videoActionPlanGenerator.generate(duration == null ? 4 : duration, promptKoEn);

        List<String> lines = new ArrayList<>();
        lines.add("Generate a " + (duration == null ? 4 : duration) + "-second single continuous shot video from the provided start image. No cuts, no time jumps.");
        lines.add("Action plan: " + requireEn(actionPlanEn));

        String cameraLine = "Camera motion: " + safeOrNone(cameraMotionEn) + ".";
        if (!motionDescriptionEn.isBlank()) {
            cameraLine += " " + ensurePeriod(motionDescriptionEn);
        }
        lines.add(cameraLine);
        lines.add("Keep character identity, outfits, lighting, and location consistent. No text, no watermark, no logo.");
        lines.add("Style: " + safeOrNone(style) + ". Time: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
        lines.add("Hold the final pose for the last half-second.");

        if (read(settings, "endShotNodeId") != null) {
            lines.add("End should gently approach the end shot composition (no hard cut).");
        }

        return joinAndValidate(lines);
    }

    private String resolveCameraMotionEn(Map<String, Object> settings) {
        String fromKey = PresetFragments.cameraMotionEn(read(settings, "cameraMotionKey"));
        if (!fromKey.isBlank()) {
            return fromKey;
        }
        String legacy = readString(settings, "cameraMotion");
        return legacy;
    }

    private String joinAndValidate(List<String> lines) {
        String joined = String.join("\n", lines);
        if (joined.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "promptEn is empty");
        }
        return joined;
    }

    private String requireEn(String text) {
        String trimmed = Optional.ofNullable(text).map(String::trim).orElse("");
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "promptEn is empty");
        }
        return trimmed;
    }

    private String safeOrNone(String text) {
        String trimmed = Optional.ofNullable(text).map(String::trim).orElse("");
        return trimmed.isEmpty() ? DEFAULT_NONE : trimmed;
    }

    private Object read(Map<String, Object> settings, String key) {
        if (settings == null || key == null) {
            return null;
        }
        return settings.get(key);
    }

    private String readString(Map<String, Object> settings, String key) {
        Object value = read(settings, key);
        if (value == null) {
            return "";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "" : text;
    }

    private Integer readInt(Map<String, Object> settings, String key) {
        Object value = read(settings, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Map<String, Object> settings, String key) {
        Object value = read(settings, key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String text = item.toString().trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private String ensurePeriod(String sentence) {
        String trimmed = sentence.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            return trimmed;
        }
        return trimmed + ".";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
