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

    public static String filmLookFragment(Object filmLookKey) {
        FilmLookKey key = FilmLookKey.from(filmLookKey);
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
        PHOTO_REAL("photorealistic, realistic skin texture, natural material surfaces, physically plausible lighting"),
        ANIME_2D("2D anime illustration, clean ink line art, cel-shaded flat colors, vibrant saturated palette, subtle rim lighting on characters"),
        STYLIZED_3D("stylized 3D animated feature-film look, smooth subsurface-scattered skin, soft ambient occlusion, rounded appealing shapes"),
        WATERCOLOR_ILLUSTRATION("watercolor illustration, soft washes, subtle paper texture, loose wet-on-wet edges, visible pigment granulation, delicate color bleeds"),
        OIL_PAINT_ILLUSTRATION("oil paint illustration, textured brush strokes, thick impasto highlights, rich color mixing, canvas weave texture visible in shadow areas");

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
            key = normalizeStyleKeyAlias(key);
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static String normalizeStyleKeyAlias(String key) {
            return switch (key) {
                // Legacy aliases (1~2 sprints compatibility)
                case "CINEMATIC_REAL" -> "PHOTO_REAL";
                case "ANIME" -> "ANIME_2D";
                case "PIXAR" -> "STYLIZED_3D";
                // These are film-look keys; for now degrade safely to a media/rendering style
                case "NOIR", "DOCUMENTARY" -> "PHOTO_REAL";
                default -> key;
            };
        }
    }

    public enum TimeOfDayKey {
        DAWN("early dawn, pale pink-orange sky gradient, soft diffused pre-sunrise light, long blue-tinted shadows"),
        DAY("bright midday, clear overhead sunlight, short crisp shadows, neutral white balance"),
        DUSK("golden hour, warm amber sunset light raking at a low angle, long stretched shadows, rich orange-pink sky"),
        NIGHT("nighttime, deep blue-black sky, cool moonlight with isolated warm practical light sources, visible ambient glow");

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
            key = normalizeTimeOfDayKeyAlias(key);
            try {
                return valueOf(key);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static String normalizeTimeOfDayKeyAlias(String key) {
            return switch (key) {
                case "MORNING" -> "DAWN";
                case "EVENING" -> "DUSK";
                default -> key;
            };
        }
    }

    public enum MoodKey {
        NEUTRAL("natural color grade, balanced lighting, moderate contrast"),
        COZY("warm color grade, soft diffused lighting, gentle contrast, soft highlights"),
        LONELY("cooler tones, slightly desaturated, more negative space, calm atmosphere"),
        TENSE("low-key lighting, higher contrast, cooler grade, subtle shadow emphasis, subtle film grain"),
        HOPEFUL("bright high-key lighting, vibrant but natural colors, soft highlights, gentle lens flare"),
        DARK("desaturated cool palette, dim ambience, muted color palette, hazy atmosphere, vignette edges");

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

    public enum FilmLookKey {
        CINEMATIC_MODERN("cinematic modern film look, filmic color grading, subtle film grain, natural highlight roll-off, gentle lens bloom");

        private final String fragment;

        FilmLookKey(String fragment) {
            this.fragment = fragment;
        }

        public String fragment() {
            return fragment;
        }

        public static FilmLookKey from(Object raw) {
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
        WIDE("wide shot, full environment visible, subject occupies less than a third of the frame"),
        MEDIUM("medium shot, waist-up framing, balanced subject-to-environment ratio"),
        CLOSE_UP("close-up, head and shoulders tightly framed, background softly blurred"),
        EXTREME_CLOSE_UP("extreme close-up, single facial feature or object detail fills the entire frame"),
        OTS("over-the-shoulder shot, foreground shoulder softly blurred, subject in sharp focus"),
        POV("POV first-person perspective, hands or held object visible in foreground"),
        HIGH_ANGLE("high-angle shot, camera looking down at the subject, subject appears smaller in the environment"),
        LOW_ANGLE("low-angle shot, camera looking up at the subject, subject appears powerful and dominant");

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
        STATIC("static locked-off camera, no movement, rock-steady frame"),
        SLOW_ZOOM_IN("slow gradual zoom in, gently narrowing the frame over the full duration"),
        ZOOM_OUT("steady zoom out, slowly revealing more of the surrounding environment"),
        PAN_LR("smooth pan from left to right at a constant speed, following the action"),
        TILT_UP("smooth tilt upward, gradually revealing the scene from bottom to top");

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
