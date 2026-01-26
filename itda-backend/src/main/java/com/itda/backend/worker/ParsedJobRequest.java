package com.itda.backend.worker;

import java.util.Map;

public record ParsedJobRequest(String prompt, Map<String, Object> settings) {
}
