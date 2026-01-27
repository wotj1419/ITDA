package com.itda.backend.ai.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates Korean(or mixed) text into a single English sentence.
 */
public interface KoEnTranslator {

    /**
     * @param text input text (Korean or mixed). Blank input should return blank output.
     * @return single English sentence (no bullets/JSON), or blank if input is blank.
     */
    String toEnglishOneSentence(String text);

    /**
     * Batch translation helper. Implementations may override to reduce provider calls.
     *
     * @param texts key -> input text (Korean or mixed). Blank inputs return blank outputs.
     * @return key -> single English sentence (no bullets/JSON).
     */
    default Map<String, String> toEnglishOneSentenceMany(Map<String, String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : texts.entrySet()) {
            result.put(entry.getKey(), toEnglishOneSentence(entry.getValue()));
        }
        return result;
    }
}
