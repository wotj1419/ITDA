package com.itda.backend.ai.veo;

import java.util.Map;

public record VeoRequest(String prompt,
                         Map<String, Object> settings,
                         VeoImage firstFrame,
                         VeoImage lastFrame) {
    public VeoRequest(String prompt, Map<String, Object> settings) {
        this(prompt, settings, null, null);
    }
}
