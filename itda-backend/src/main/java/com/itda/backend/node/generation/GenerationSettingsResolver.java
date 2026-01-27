package com.itda.backend.node.generation;

import com.itda.backend.ai.prompt.PresetFragments;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.node.domain.NodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class GenerationSettingsResolver {

    private static final String KEY_ASPECT_RATIO = "aspectRatio";

    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final Set<String> VIDEO_ALLOWED_ASPECT_RATIOS = Set.of("16:9", "9:16");
    private static final Set<Integer> VIDEO_ALLOWED_DURATIONS = Set.of(4, 6, 8);

    private static final String DEFAULT_GRID_LAYOUT = "2x2";
    private static final List<String> DEFAULT_GRID_SHOT_TYPES = List.of("WIDE", "MEDIUM", "CLOSE_UP", "OTS");

    public Map<String, Object> resolve(
            NodeType nodeType,
            Map<String, Object> existingSettings,
            Map<String, Object> requestSettings,
            Map<String, Object> activeMasterSettings
    ) {
        if (nodeType == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Map<String, Object> merged = new LinkedHashMap<>();
        if (existingSettings != null) {
            merged.putAll(existingSettings);
        }
        if (requestSettings != null) {
            merged.putAll(requestSettings);
        }

        normalizeLookKeys(merged);
        fillLookKeysFromLegacyLabelsIfMissing(merged);
        applyLookInheritanceIfNeeded(nodeType, merged, activeMasterSettings);
        normalizeLookKeys(merged);
        applyGlobalDefaults(nodeType, merged);
        normalizeByType(nodeType, merged);
        validateByType(nodeType, merged);

        return merged;
    }

    private void applyLookInheritanceIfNeeded(NodeType nodeType, Map<String, Object> target, Map<String, Object> master) {
        if (master == null || master.isEmpty()) {
            return;
        }
        if (nodeType == NodeType.MASTER) {
            return;
        }
        copyIfMissing(target, master, "styleKey");
        copyIfMissing(target, master, "timeOfDayKey");
        copyIfMissing(target, master, "moodKey");
        copyIfMissing(target, master, "objectIds");
        copyIfMissing(target, master, KEY_ASPECT_RATIO);
    }

    private void applyGlobalDefaults(NodeType nodeType, Map<String, Object> settings) {
        putIfMissing(settings, KEY_ASPECT_RATIO, DEFAULT_ASPECT_RATIO);
        putIfMissing(settings, "styleKey", "PHOTO_REAL");
        putIfMissing(settings, "timeOfDayKey", "DAY");
        putIfMissing(settings, "moodKey", "NEUTRAL");

        switch (nodeType) {
            case MASTER -> {
            }
            case GRID -> {
                putIfMissing(settings, "gridMode", "SHOT_VARIATIONS");
                putIfMissing(settings, "layout", DEFAULT_GRID_LAYOUT);
                putIfMissing(settings, "shotTypes", new ArrayList<>(DEFAULT_GRID_SHOT_TYPES));
            }
            case SHOT -> {
                putIfMissing(settings, "gridCellIndex", 0);
                putIfMissing(settings, "shotType", "WIDE");
                putIfMissing(settings, "expressionKey", "NEUTRAL");
            }
            case VIDEO -> {
                putIfMissing(settings, "duration", 4);
                putIfMissing(settings, "cameraMotionKey", "STATIC");
                putIfMissing(settings, "provider", "VEO_3_1");
            }
            case SCENE_HEADER -> {
            }
        }
    }

    private void normalizeByType(NodeType nodeType, Map<String, Object> settings) {
        normalizeLookKeys(settings);

        if (nodeType == NodeType.MASTER) {
            if (settings.containsKey("objectIds") && settings.get("objectIds") == null) {
                settings.put("objectIds", List.of());
            }
        }
        if (nodeType == NodeType.VIDEO) {
            Integer duration = readInt(settings, "duration");
            if (duration != null) {
                settings.put("duration", duration);
            }
            String aspectRatio = normalizeAspectRatio(readString(settings, KEY_ASPECT_RATIO));
            settings.put(KEY_ASPECT_RATIO, aspectRatio.isEmpty() ? DEFAULT_ASPECT_RATIO : aspectRatio);
            normalizeProvider(settings);
            normalizeStringKey(settings, "cameraMotionKey");
        }

        if (nodeType == NodeType.GRID) {
            normalizeStringKey(settings, "gridMode");
            String layout = normalizeLayout(readString(settings, "layout"));
            settings.put("layout", layout);

            String gridMode = readString(settings, "gridMode");
            if (!gridMode.isEmpty() && !gridMode.equalsIgnoreCase("SHOT_VARIATIONS")) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "GRID gridMode not supported in P0");
            }

            int panelCount = GridLayout.parse(layout).panelCount();
            List<String> shotTypes = normalizeShotTypes(settings.get("shotTypes"));
            List<String> corrected = autoCorrectShotTypes(panelCount, shotTypes);
            settings.put("shotTypes", corrected);
        }
    }

    private void fillLookKeysFromLegacyLabelsIfMissing(Map<String, Object> settings) {
        if (settings == null || settings.isEmpty()) {
            return;
        }

        removeIfBlank(settings, "styleKey");
        removeIfBlank(settings, "timeOfDayKey");
        removeIfBlank(settings, "moodKey");

        if (isMissing(settings, "styleKey")) {
            String legacy = readString(settings, "style");
            String mapped = mapStyleToStyleKey(legacy);
            if (mapped != null) {
                settings.put("styleKey", mapped);
            }
        }

        if (isMissing(settings, "timeOfDayKey")) {
            String legacy = readString(settings, "timeOfDay");
            String mapped = mapTimeOfDayToKey(legacy);
            if (mapped != null) {
                settings.put("timeOfDayKey", mapped);
            }
        }

        if (isMissing(settings, "moodKey")) {
            String legacy = readString(settings, "mood");
            String mapped = mapMoodToKey(legacy);
            if (mapped != null) {
                settings.put("moodKey", mapped);
            }
        }
    }

    private void removeIfBlank(Map<String, Object> settings, String key) {
        if (!settings.containsKey(key)) {
            return;
        }
        Object value = settings.get(key);
        if (value == null) {
            settings.remove(key);
            return;
        }
        if (value.toString().trim().isEmpty()) {
            settings.remove(key);
        }
    }

    private boolean isMissing(Map<String, Object> settings, String key) {
        if (!settings.containsKey(key)) {
            return true;
        }
        Object value = settings.get(key);
        if (value == null) {
            return true;
        }
        return value.toString().trim().isEmpty();
    }

    private String mapStyleToStyleKey(String legacy) {
        if (legacy == null || legacy.isBlank()) {
            return null;
        }
        PresetFragments.StyleKey parsed = PresetFragments.StyleKey.from(legacy);
        if (parsed != null) {
            return parsed.name();
        }

        String trimmed = legacy.trim();
        return switch (trimmed) {
            case "실사" -> "PHOTO_REAL";
            case "애니메이션", "애니" -> "ANIME_2D";
            case "픽사" -> "STYLIZED_3D";
            case "수채화" -> "WATERCOLOR_ILLUSTRATION";
            case "유화" -> "OIL_PAINT_ILLUSTRATION";
            default -> null;
        };
    }

    private String mapTimeOfDayToKey(String legacy) {
        if (legacy == null || legacy.isBlank()) {
            return null;
        }
        PresetFragments.TimeOfDayKey parsed = PresetFragments.TimeOfDayKey.from(legacy);
        if (parsed != null) {
            return parsed.name();
        }

        String trimmed = legacy.trim();
        return switch (trimmed) {
            case "아침" -> "DAWN";
            case "낮" -> "DAY";
            case "저녁" -> "DUSK";
            case "밤" -> "NIGHT";
            default -> null;
        };
    }

    private String mapMoodToKey(String legacy) {
        if (legacy == null || legacy.isBlank()) {
            return null;
        }
        PresetFragments.MoodKey parsed = PresetFragments.MoodKey.from(legacy);
        if (parsed != null) {
            return parsed.name();
        }

        String trimmed = legacy.trim();
        return switch (trimmed) {
            case "편안" -> "COZY";
            case "고독" -> "LONELY";
            case "긴장" -> "TENSE";
            case "행복" -> "HOPEFUL";
            case "우울" -> "DARK";
            default -> null;
        };
    }

    private void normalizeLookKeys(Map<String, Object> settings) {
        if (settings == null || settings.isEmpty()) {
            return;
        }

        removeIfBlank(settings, "styleKey");
        removeIfBlank(settings, "timeOfDayKey");
        removeIfBlank(settings, "moodKey");

        normalizeStringKey(settings, "styleKey");
        normalizeStringKey(settings, "timeOfDayKey");
        normalizeStringKey(settings, "moodKey");

        String styleKey = readString(settings, "styleKey");
        if (!styleKey.isEmpty()) {
            PresetFragments.StyleKey parsed = PresetFragments.StyleKey.from(styleKey);
            if (parsed != null) {
                settings.put("styleKey", parsed.name());
            } else {
                String mapped = mapStyleToStyleKey(styleKey);
                if (mapped != null) {
                    settings.put("styleKey", mapped);
                } else {
                    settings.remove("styleKey");
                }
            }
        }

        String timeOfDayKey = readString(settings, "timeOfDayKey");
        if (!timeOfDayKey.isEmpty()) {
            PresetFragments.TimeOfDayKey parsed = PresetFragments.TimeOfDayKey.from(timeOfDayKey);
            if (parsed != null) {
                settings.put("timeOfDayKey", parsed.name());
            } else {
                String mapped = mapTimeOfDayToKey(timeOfDayKey);
                if (mapped != null) {
                    settings.put("timeOfDayKey", mapped);
                } else {
                    settings.remove("timeOfDayKey");
                }
            }
        }

        String moodKey = readString(settings, "moodKey");
        if (!moodKey.isEmpty()) {
            PresetFragments.MoodKey parsed = PresetFragments.MoodKey.from(moodKey);
            if (parsed != null) {
                settings.put("moodKey", parsed.name());
            } else {
                String mapped = mapMoodToKey(moodKey);
                if (mapped != null) {
                    settings.put("moodKey", mapped);
                } else {
                    settings.remove("moodKey");
                }
            }
        }
    }

    private void validateByType(NodeType nodeType, Map<String, Object> settings) {
        if (nodeType != NodeType.VIDEO) {
            return;
        }

        Integer duration = readInt(settings, "duration");
        if (duration == null || !VIDEO_ALLOWED_DURATIONS.contains(duration)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "VIDEO duration must be 4, 6, or 8");
        }

        String aspectRatio = normalizeAspectRatio(readString(settings, KEY_ASPECT_RATIO));
        if (aspectRatio.isEmpty()) {
            aspectRatio = DEFAULT_ASPECT_RATIO;
        }
        if (!VIDEO_ALLOWED_ASPECT_RATIOS.contains(aspectRatio)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "VIDEO aspectRatio must be 16:9 or 9:16");
        }
        settings.put(KEY_ASPECT_RATIO, aspectRatio);
    }

    private void copyIfMissing(Map<String, Object> target, Map<String, Object> source, String key) {
        if (target.containsKey(key) && !isBlankValue(target.get(key))) {
            return;
        }
        if (!source.containsKey(key)) {
            return;
        }
        Object value = source.get(key);
        if (isBlankValue(value)) {
            return;
        }
        target.put(key, value);
    }

    private void putIfMissing(Map<String, Object> settings, String key, Object value) {
        if (settings.containsKey(key) && !isBlankValue(settings.get(key))) {
            return;
        }
        settings.put(key, value);
    }

    private String readString(Map<String, Object> settings, String key) {
        if (settings == null || key == null) {
            return "";
        }
        Object value = settings.get(key);
        if (value == null) {
            return "";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "" : text;
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.trim().isEmpty();
        }
        return false;
    }

    private Integer readInt(Map<String, Object> settings, String key) {
        if (settings == null || key == null) {
            return null;
        }
        Object value = settings.get(key);
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

    private void normalizeStringKey(Map<String, Object> settings, String key) {
        String value = readString(settings, key);
        if (value.isEmpty()) {
            return;
        }
        settings.put(key, value.trim().toUpperCase(Locale.ROOT));
    }

    private void normalizeProvider(Map<String, Object> settings) {
        String provider = readString(settings, "provider");
        if (provider.isEmpty()) {
            return;
        }
        String trimmed = provider.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("veo-")) {
            settings.put("provider", trimmed);
            return;
        }
        settings.put("provider", trimmed.toUpperCase(Locale.ROOT));
    }

    private String normalizeAspectRatio(String value) {
        String trimmed = Optional.ofNullable(value).map(String::trim).orElse("");
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed;
    }

    private String normalizeLayout(String value) {
        String trimmed = Optional.ofNullable(value).map(String::trim).orElse("");
        if (trimmed.isEmpty()) {
            return DEFAULT_GRID_LAYOUT;
        }
        GridLayout parsed = GridLayout.tryParse(trimmed);
        return parsed == null ? DEFAULT_GRID_LAYOUT : parsed.normalized();
    }

    private List<String> normalizeShotTypes(Object raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item == null) {
                    continue;
                }
                String text = item.toString().trim();
                if (!text.isEmpty()) {
                    result.add(text.toUpperCase(Locale.ROOT));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<String> autoCorrectShotTypes(int panelCount, List<String> shotTypes) {
        if (panelCount <= 0) {
            return new ArrayList<>(DEFAULT_GRID_SHOT_TYPES);
        }
        List<String> base = shotTypes == null ? List.of() : shotTypes;
        List<String> corrected = new ArrayList<>();
        if (!base.isEmpty()) {
            corrected.addAll(base);
        }

        if (corrected.size() < panelCount) {
            int i = 0;
            while (corrected.size() < panelCount) {
                corrected.add(DEFAULT_GRID_SHOT_TYPES.get(i % DEFAULT_GRID_SHOT_TYPES.size()));
                i++;
            }
        }

        if (corrected.size() > panelCount) {
            return new ArrayList<>(corrected.subList(0, panelCount));
        }

        return corrected;
    }
}
