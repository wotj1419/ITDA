package com.itda.backend.node.generation;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.node.domain.NodeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationSettingsResolverTest {

    private final GenerationSettingsResolver resolver = new GenerationSettingsResolver();

    @Test
    void resolve_videoDefaults_appliesDefaultsAndValidates() {
        Map<String, Object> resolved = resolver.resolve(
                NodeType.VIDEO,
                Map.of(),
                Map.of("startShotNodeId", 10L),
                Map.of()
        );

        assertThat(resolved.get("duration")).isEqualTo(4);
        assertThat(resolved.get("cameraMotionKey")).isEqualTo("STATIC");
        assertThat(resolved.get("provider")).isEqualTo("VEO_3_1");
        assertThat(resolved.get("aspectRatio")).isEqualTo("16:9");
    }

    @Test
    void resolve_videoInvalidDuration_throws() {
        assertThatThrownBy(() -> resolver.resolve(
                NodeType.VIDEO,
                Map.of(),
                Map.of("duration", 5),
                Map.of()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void resolve_videoInvalidAspectRatio_throws() {
        assertThatThrownBy(() -> resolver.resolve(
                NodeType.VIDEO,
                Map.of(),
                Map.of("aspectRatio", "1:1", "duration", 4),
                Map.of()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void resolve_gridAutoCorrectShotTypes_fillsOrTruncates() {
        Map<String, Object> resolvedFill = resolver.resolve(
                NodeType.GRID,
                Map.of(),
                Map.of(
                        "layout", "2x3",
                        "shotTypes", List.of("WIDE", "MEDIUM", "CLOSE_UP", "OTS")
                ),
                Map.of()
        );
        assertThat(resolvedFill.get("layout")).isEqualTo("2x3");
        assertThat(resolvedFill.get("shotTypes"))
                .asList()
                .hasSize(6);

        Map<String, Object> resolvedTruncate = resolver.resolve(
                NodeType.GRID,
                Map.of(),
                Map.of(
                        "layout", "2x2",
                        "shotTypes", List.of("WIDE", "MEDIUM", "CLOSE_UP", "OTS", "POV", "LOW_ANGLE")
                ),
                Map.of()
        );
        assertThat(resolvedTruncate.get("shotTypes"))
                .asList()
                .containsExactly("WIDE", "MEDIUM", "CLOSE_UP", "OTS");
    }

    @Test
    void resolve_gridStoryBeats_throwsInP0() {
        assertThatThrownBy(() -> resolver.resolve(
                NodeType.GRID,
                Map.of(),
                Map.of("gridMode", "STORY_BEATS", "layout", "1x4"),
                Map.of()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void resolve_inheritsLookKeysFromActiveMasterWhenMissing() {
        Map<String, Object> resolved = resolver.resolve(
                NodeType.SHOT,
                Map.of(),
                Map.of("gridCellIndex", 1),
                Map.of(
                        "styleKey", "NOIR",
                        "timeOfDayKey", "NIGHT",
                        "moodKey", "DARK",
                        "aspectRatio", "9:16"
                )
        );

        assertThat(resolved.get("styleKey")).isEqualTo("NOIR");
        assertThat(resolved.get("timeOfDayKey")).isEqualTo("NIGHT");
        assertThat(resolved.get("moodKey")).isEqualTo("DARK");
        assertThat(resolved.get("aspectRatio")).isEqualTo("9:16");
    }
}
