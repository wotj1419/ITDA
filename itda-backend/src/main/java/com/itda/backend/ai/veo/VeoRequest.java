package com.itda.backend.ai.veo;

import java.util.Map;

public record VeoRequest(String prompt, Map<String, Object> settings) {
}
