package com.itda.backend.ai.prompt;

import com.itda.backend.node.domain.NodeType;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptRendererTest {

    private final KoEnTranslator translator = text -> {
        if (text == null || text.isBlank()) {
            return "";
        }
        return "EN(" + text.trim() + ")";
    };

    private final PromptRenderer renderer = new PromptRenderer(translator, new VideoActionPlanGenerator());

    @Test
    void render_video_includesHoldAndOptionalEndLine() {
        Scene scene = Scene.builder().title("씬").description("설명").build();
        String promptEn = renderer.render(
                NodeType.VIDEO,
                scene,
                "선택한 샷에서 시작하는 단일 연속 숏 영상",
                Map.of(
                        "duration", 6,
                        "cameraMotionKey", "STATIC",
                        "motionDescriptionKo", "천천히 줌인",
                        "styleKey", "CINEMATIC_REAL",
                        "timeOfDayKey", "DAY",
                        "moodKey", "NEUTRAL",
                        "endShotNodeId", 99L
                )
        );

        assertThat(promptEn).contains("Hold the final pose for the last half-second.");
        assertThat(promptEn).contains("End should gently approach the end shot composition (no hard cut).");
        assertThat(promptEn).contains("Generate a 6-second single continuous shot video");
        assertThat(promptEn).contains("Action plan: EN(선택한 샷에서 시작하는 단일 연속 숏 영상). Then a brief follow-up reaction");
    }

    @Test
    void render_grid_shotVariations_rendersPanels() {
        Scene scene = Scene.builder().title("씬").description("설명").build();
        String promptEn = renderer.render(
                NodeType.GRID,
                scene,
                "같은 장면을 4칸 그리드로",
                Map.of(
                        "layout", "2x2",
                        "shotTypes", List.of("WIDE", "MEDIUM", "CLOSE_UP", "OTS"),
                        "aspectRatio", "16:9",
                        "compositionHintKo", "인물 중심",
                        "styleKey", "CINEMATIC_REAL",
                        "timeOfDayKey", "DAY",
                        "moodKey", "NEUTRAL"
                )
        );

        assertThat(promptEn).contains("Create ONE storyboard grid image with 2x2 panels");
        assertThat(promptEn).contains("Panel 1:");
        assertThat(promptEn).contains("Panel 4:");
        assertThat(promptEn).contains("Composition note: EN(인물 중심).");
    }

    @Test
    void render_grid_storyBeats_throwsInP0() {
        Scene scene = Scene.builder().title("씬").description("설명").build();
        assertThatThrownBy(() -> renderer.render(
                NodeType.GRID,
                scene,
                "스토리 비트",
                Map.of("gridMode", "STORY_BEATS", "layout", "1x4")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
