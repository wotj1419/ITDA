package com.itda.backend.ai.prompt;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VideoActionPlanGenerator {

    public String generate(int durationSeconds, String promptKoEn) {
        String base = Optional.ofNullable(promptKoEn).map(String::trim).orElse("");
        if (base.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "promptKoEn is empty");
        }
        String normalizedBase = ensurePeriod(base);

        return switch (durationSeconds) {
            case 4 -> normalizedBase;
            case 6 -> normalizedBase + " Then a brief follow-up reaction completes the moment.";
            case 8 -> normalizedBase + " Then two small follow-up beats happen, and the action settles into a neutral pose.";
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

