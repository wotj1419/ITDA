package com.itda.backend.node.generation;

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

        applyLookInheritanceIfNeeded(nodeType, merged, activeMasterSettings);
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

        switch (nodeType) {
            case MASTER -> {
                putIfMissing(settings, "styleKey", "CINEMATIC_REAL");
                putIfMissing(settings, "timeOfDayKey", "DAY");
                putIfMissing(settings, "moodKey", "NEUTRAL");
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
        if (target.containsKey(key) && target.get(key) != null) {
            return;
        }
        if (!source.containsKey(key)) {
            return;
        }
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        target.put(key, value);
    }

    private void putIfMissing(Map<String, Object> settings, String key, Object value) {
        if (settings.containsKey(key) && settings.get(key) != null) {
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
