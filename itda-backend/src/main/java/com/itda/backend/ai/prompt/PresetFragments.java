package com.itda.backend.ai.prompt;

import java.util.Locale;

public final class PresetFragments {

    private PresetFragments() {
    }

    public static String styleFragment(Object styleKey) {
        StyleKey key = StyleKey.from(styleKey);
        return key == null ? "" : key.fragment();
    }

    public static String timeOfDayFragment(Object timeOfDayKey) {
        TimeOfDayKey key = TimeOfDayKey.from(timeOfDayKey);
        return key == null ? "" : key.fragment();
    }

    public static String moodFragment(Object moodKey) {
        MoodKey key = MoodKey.from(moodKey);
        return key == null ? "" : key.fragment();
    }

    public static String shotTypeEn(Object shotTypeKey) {
        ShotTypeKey key = ShotTypeKey.from(shotTypeKey);
        return key == null ? "" : key.en();
    }

    public static String expressionEn(Object expressionKey) {
        ExpressionKey key = ExpressionKey.from(expressionKey);
        return key == null ? "" : key.en();
    }

    public static String cameraMotionEn(Object cameraMotionKey) {
        CameraMotionKey key = CameraMotionKey.from(cameraMotionKey);
        return key == null ? "" : key.en();
    }

    private static String normalizeKey(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.toUpperCase(Locale.ROOT);
    }

    public enum StyleKey {
        CINEMATIC_REAL("cinematic realistic"),
        ANIME("anime style"),
        PIXAR("Pixar-like 3D animation"),
        NOIR("film noir"),
        DOCUMENTARY("documentary style");

        private final String fragment;

        StyleKey(String fragment) {
            this.fragment = fragment;
        }

        public String fragment() {
            return fragment;
        }

        public static StyleKey from(Object raw) {
            String key = normalizeKey(raw);
            if (key == null) {
                return null;
            }
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum TimeOfDayKey {
        DAWN("dawn"),
        DAY("daytime"),
        DUSK("dusk"),
        NIGHT("night");

        private final String fragment;

        TimeOfDayKey(String fragment) {
            this.fragment = fragment;
        }

        public String fragment() {
            return fragment;
        }

        public static TimeOfDayKey from(Object raw) {
            String key = normalizeKey(raw);
            if (key == null) {
                return null;
            }
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum MoodKey {
        NEUTRAL("neutral"),
        COZY("cozy"),
        LONELY("lonely"),
        TENSE("tense"),
        HOPEFUL("hopeful"),
        DARK("dark");

        private final String fragment;

        MoodKey(String fragment) {
            this.fragment = fragment;
        }

        public String fragment() {
            return fragment;
        }

        public static MoodKey from(Object raw) {
            String key = normalizeKey(raw);
            if (key == null) {
                return null;
            }
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum ShotTypeKey {
        WIDE("wide shot"),
        MEDIUM("medium shot"),
        CLOSE_UP("close-up"),
        EXTREME_CLOSE_UP("extreme close-up"),
        OTS("over-the-shoulder shot"),
        POV("POV shot"),
        HIGH_ANGLE("high-angle shot"),
        LOW_ANGLE("low-angle shot");

        private final String en;

        ShotTypeKey(String en) {
            this.en = en;
        }

        public String en() {
            return en;
        }

        public static ShotTypeKey from(Object raw) {
            String key = normalizeKey(raw);
            if (key == null) {
                return null;
            }
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum ExpressionKey {
        NEUTRAL("neutral"),
        SMILE("smiling"),
        SAD("sad"),
        SURPRISED("surprised"),
        ANGRY("angry"),
        BLANK("blank");

        private final String en;

        ExpressionKey(String en) {
            this.en = en;
        }

        public String en() {
            return en;
        }

        public static ExpressionKey from(Object raw) {
            String key = normalizeKey(raw);
            if (key == null) {
                return null;
            }
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum CameraMotionKey {
        STATIC("static camera"),
        SLOW_ZOOM_IN("slow zoom in"),
        ZOOM_OUT("zoom out"),
        PAN_LR("pan left to right"),
        TILT_UP("tilt up");

        private final String en;

        CameraMotionKey(String en) {
            this.en = en;
        }

        public String en() {
            return en;
        }

        public static CameraMotionKey from(Object raw) {
            String key = normalizeKey(raw);
            if (key == null) {
                return null;
            }
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}

