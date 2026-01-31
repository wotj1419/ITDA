package com.itda.backend.ai.prompt;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.node.domain.NodeType;
import com.itda.backend.node.generation.GridLayout;
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
    private static final int TIMELINE_CUT_INTERVAL_SECONDS = 2;

    private final KoEnTranslator koEnTranslator;
    private final VideoActionPlanGenerator videoActionPlanGenerator;

    public String render(NodeType nodeType, Scene scene, String promptEnBase, Map<String, Object> settings) {
        if (nodeType == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String promptEnTrimmed = Optional.ofNullable(promptEnBase).map(String::trim).orElse("");
        if (promptEnTrimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty");
        }

        return switch (nodeType) {
            case MASTER -> renderMaster(scene, promptEnTrimmed, settings);
            case GRID -> {
                String gridMode = readString(settings, "gridMode");
                if (gridMode.isEmpty() || gridMode.equalsIgnoreCase("SHOT_VARIATIONS")) {
                    yield renderGridShotVariations(scene, promptEnTrimmed, settings);
                }
                if (gridMode.equalsIgnoreCase("STORY_BEATS")) {
                    yield renderGridStoryBeats(scene, promptEnTrimmed, settings);
                }
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "GRID gridMode must be SHOT_VARIATIONS or STORY_BEATS");
            }
            case SHOT -> renderShot(scene, promptEnTrimmed, settings);
            case VIDEO -> renderVideo(scene, promptEnTrimmed, settings);
            case SCENE_HEADER -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
    }

    private String renderMaster(Scene scene, String promptEnBase, Map<String, Object> settings) {
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(Map.of(
                "sceneTitle", scene == null ? "" : safe(scene.getTitle()),
                "sceneDescription", scene == null ? "" : safe(scene.getDescription())
        ));
        String sceneTitleEn = translated.getOrDefault("sceneTitle", "");
        String sceneDescriptionEn = translated.getOrDefault("sceneDescription", "");

        String aspectRatio = readString(settings, "aspectRatio");
        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String filmLook = PresetFragments.filmLookFragment(read(settings, "filmLookKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        List<String> lines = new ArrayList<>();
        lines.add("A " + safeOrNone(filmLook) + " " + safeOrNone(style) + " wide establishing shot of " + requireEn(promptEnBase) + ".");
        lines.add("This is an opening establishing still frame with no temporal progression or passing-by.");
        lines.add("Set in the scene \"" + safeOrNone(sceneTitleEn) + "\" — " + safeOrNone(sceneDescriptionEn) + ".");
        lines.add("Lighting: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
        lines.add("Camera: 24-35mm wide lens, deep focus (establishing shot).");
        lines.add("Maintain consistent character identity, outfits, lighting, and key props across all shots.");
        lines.add("No text, no subtitles, no watermark, no logo.");
        lines.add("Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderGridShotVariations(Scene scene, String promptEnBase, Map<String, Object> settings) {
        String layout = readString(settings, "layout");
        String aspectRatio = readString(settings, "aspectRatio");
        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("compositionHintKo", readString(settings, "compositionHintKo"));
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(translationInput);
        String compositionHintEn = translated.getOrDefault("compositionHintKo", "");

        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String filmLook = PresetFragments.filmLookFragment(read(settings, "filmLookKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        List<String> shotTypes = readStringList(settings, "shotTypes");
        List<String> lines = new ArrayList<>();
        lines.add("Create ONE storyboard grid image with " + safeOrNone(layout) + " panels, all showing the SAME moment in the SAME scene (not sequential).");
        lines.add("Base content: " + requireEn(promptEnBase) + ".");
        lines.add("Panels must differ only by camera framing:");
        for (int i = 0; i < shotTypes.size(); i++) {
            String shotTypeEn = PresetFragments.shotTypeEn(shotTypes.get(i));
            lines.add("Panel " + (i + 1) + ": " + safeOrNone(shotTypeEn));
        }
        lines.add("All panels share consistent characters, outfits, lighting, and location.");
        lines.add("Rendered in a " + safeOrNone(filmLook) + " " + safeOrNone(style) + " look under " + safeOrNone(time) + " lighting with a " + safeOrNone(mood) + " feel.");
        lines.add("Composition note: " + safeOrNone(compositionHintEn) + ".");
        lines.add("No captions, no text, no watermark, no logo. Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderGridStoryBeats(Scene scene, String promptEnBase, Map<String, Object> settings) {
        String layout = readString(settings, "layout");
        String aspectRatio = readString(settings, "aspectRatio");
        int panelCount = GridLayout.parse(layout).panelCount();
        List<String> beatsKo = ensureBeatCount(readStringList(settings, "beatsKo"), panelCount);

        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("continuityRulesKo", readString(settings, "continuityRulesKo"));
        for (int i = 0; i < beatsKo.size(); i++) {
            translationInput.put("beat" + i, beatsKo.get(i));
        }
        Map<String, String> translated = koEnTranslator.toEnglishMultiSentenceMany(translationInput);
        String continuityRulesEn = translated.getOrDefault("continuityRulesKo", "");

        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String filmLook = PresetFragments.filmLookFragment(read(settings, "filmLookKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        List<String> lines = new ArrayList<>();
        lines.add("Create ONE storyboard grid image with " + safeOrNone(layout) + " panels that depict sequential still frames sampled every " + TIMELINE_CUT_INTERVAL_SECONDS + " seconds.");
        lines.add("Base content: " + requireEn(promptEnBase) + ".");
        lines.add("Panels 1.." + panelCount + " are timeline cuts (single still frames):");
        for (int i = 0; i < beatsKo.size(); i++) {
            String beatEn = translated.getOrDefault("beat" + i, "").trim();
            if (beatEn.isEmpty()) {
                continue;
            }
            lines.add((i + 1) + ") (" + formatTimelineRangeLabel(i) + ") " + beatEn);
        }
        lines.add("Each panel is a single frozen moment with no transitions or motion blur.");
        lines.add("Keep continuity across panels with the same characters, outfits, lighting, and location.");
        lines.add("Rendered in a " + safeOrNone(filmLook) + " " + safeOrNone(style) + " look under " + safeOrNone(time) + " lighting with a " + safeOrNone(mood) + " feel.");
        if (!continuityRulesEn.isBlank()) {
            lines.add("Continuity rules: " + ensurePeriod(continuityRulesEn));
        }
        lines.add("No captions, no text, no watermark, no logo. Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderShot(Scene scene, String promptEnBase, Map<String, Object> settings) {
        String aspectRatio = readString(settings, "aspectRatio");
        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("detailKo", readString(settings, "detailKo"));
        translationInput.put("gridCellCutKo", readString(settings, "gridCellCutKo"));
        Map<String, String> translated = koEnTranslator.toEnglishMultiSentenceMany(translationInput);
        String detailEn = translated.getOrDefault("detailKo", "");
        String gridCellCutEn = translated.getOrDefault("gridCellCutKo", "");
        Integer gridCellIndex = readInt(settings, "gridCellIndex");

        String shotTypeEn = PresetFragments.shotTypeEn(read(settings, "shotType"));
        String expressionEn = PresetFragments.expressionEn(read(settings, "expressionKey"));

        String filmLook = PresetFragments.filmLookFragment(read(settings, "filmLookKey"));
        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        List<String> lines = new ArrayList<>();
        int safeGridCellIndex = gridCellIndex == null ? 0 : Math.max(0, gridCellIndex);
        int cellNumberHuman = safeGridCellIndex + 1;
        lines.add("A " + safeOrNone(filmLook) + " " + safeOrNone(style) + " high-quality single cinematic frame based on storyboard cell #" + cellNumberHuman + ":");
        lines.add(requireEn(promptEnBase) + ".");
        if (!safeOrNone(gridCellCutEn).equals(DEFAULT_NONE)) {
            lines.add("Match the content and framing of grid cell #" + cellNumberHuman + ": " + ensurePeriod(gridCellCutEn));
        }
        lines.add("Captured as a " + safeOrNone(shotTypeEn) + ", with the subject showing a " + safeOrNone(expressionEn) + " expression.");
        lines.add("Camera: lens and depth of field appropriate for the framing (e.g., 24-35mm wide/deep focus; 85mm close-up/shallow DoF).");
        lines.add("Lighting: " + safeOrNone(time) + ". Mood: " + safeOrNone(mood) + ".");
        if (!safeOrNone(detailEn).equals(DEFAULT_NONE)) {
            lines.add(ensurePeriod(detailEn));
        }
        lines.add("This frame matches the established master look with consistent character identity and visual continuity.");
        lines.add("No text, no watermark, no logo. Aspect ratio: " + safeOrNone(aspectRatio) + ".");

        return joinAndValidate(lines);
    }

    private String renderVideo(Scene scene, String promptEnBase, Map<String, Object> settings) {
        Integer duration = readInt(settings, "duration");
        String cameraMotionEn = resolveCameraMotionEn(settings);
        Map<String, String> translationInput = new LinkedHashMap<>();
        String motionDescriptionRaw = readString(settings, "motionDescriptionKo");
        if (motionDescriptionRaw.isBlank()) {
            motionDescriptionRaw = readString(settings, "motionDescription");
        }
        translationInput.put("motionDescription", motionDescriptionRaw);
        Map<String, String> translated = koEnTranslator.toEnglishOneSentenceMany(translationInput);
        String motionDescriptionEn = translated.getOrDefault("motionDescription", "");

        String style = PresetFragments.styleFragment(read(settings, "styleKey"));
        String filmLook = PresetFragments.filmLookFragment(read(settings, "filmLookKey"));
        String time = PresetFragments.timeOfDayFragment(read(settings, "timeOfDayKey"));
        String mood = PresetFragments.moodFragment(read(settings, "moodKey"));

        String actionPlanEn = videoActionPlanGenerator.generate(duration == null ? 4 : duration, promptEnBase);

        List<String> lines = new ArrayList<>();
        lines.add("Generate a " + (duration == null ? 4 : duration) + "-second single continuous shot video from the provided start image, with no cuts or time jumps.");
        lines.add(requireEn(actionPlanEn));

        String cameraLine = "The camera uses " + safeOrNone(cameraMotionEn) + ".";
        if (!motionDescriptionEn.isBlank()) {
            cameraLine += " " + ensurePeriod(motionDescriptionEn);
        }
        lines.add(cameraLine);
        lines.add("The scene has a " + safeOrNone(filmLook) + " " + safeOrNone(style) + " look under " + safeOrNone(time) + " lighting with a " + safeOrNone(mood) + " feel.");
        lines.add("Maintain character identity, outfits, and location consistency throughout. No text, no watermark, no logo.");
        lines.add("Hold the final pose steadily for the last half-second.");

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

    private List<String> ensureBeatCount(List<String> beats, int panelCount) {
        if (panelCount <= 0) {
            return beats;
        }
        List<String> corrected = new ArrayList<>();
        if (beats != null) {
            corrected.addAll(beats);
        }
        while (corrected.size() < panelCount) {
            corrected.add("");
        }
        if (corrected.size() > panelCount) {
            return new ArrayList<>(corrected.subList(0, panelCount));
        }
        return corrected;
    }

    private String formatTimelineRangeLabel(int index) {
        int start = index * TIMELINE_CUT_INTERVAL_SECONDS;
        int end = start + TIMELINE_CUT_INTERVAL_SECONDS;
        return start + "-" + end + "s";
    }
}
