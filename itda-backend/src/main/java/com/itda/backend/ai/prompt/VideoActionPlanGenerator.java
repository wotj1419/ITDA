package com.itda.backend.ai.prompt;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VideoActionPlanGenerator {

    public String generate(int durationSeconds, String promptKoEn, boolean hasEndFrame) {
        String base = Optional.ofNullable(promptKoEn).map(String::trim).orElse("");
        if (base.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "promptKoEn is empty");
        }
        String normalizedBase = ensurePeriod(base);

        if (!hasEndFrame) {
            return switch (durationSeconds) {
                case 4 -> normalizedBase;
                case 6 -> normalizedBase
                        + " The subject holds this action briefly, then naturally settles back"
                        + " with a subtle follow-through movement.";
                case 8 -> normalizedBase
                        + " The motion unfolds gradually over the first half;"
                        + " in the second half, the subject completes the action"
                        + " and eases into a relaxed neutral pose with gentle residual movement.";
                default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "VIDEO duration must be 4, 6, or 8");
            };
        }

        return switch (durationSeconds) {
            case 4 -> normalizedBase
                    + " Transition continuously from the start pose to the end pose with gentle ease-in/ease-out,"
                    + " without pauses or snapping.";
            case 6 -> normalizedBase
                    + " Maintain continuous motion through the transition with soft easing,"
                    + " and settle into the end pose naturally without a hard stop.";
            case 8 -> normalizedBase
                    + " Let the transition unfold gradually with smooth easing,"
                    + " then arrive at the end pose and settle gently without a snap.";
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "VIDEO duration must be 4, 6, or 8");
        };
    }

    private String ensurePeriod(String text) {
        String trimmed = text.trim();
        if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            return trimmed;
        }
        return trimmed + ".";
    }
}
