package com.itda.backend.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobRequestParserTest {

    private final JobRequestParser parser = new JobRequestParser(new ObjectMapper());

    @Test
    void parse_whenNull_throwsBusinessException() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void parse_whenBlank_throwsBusinessException() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void parse_whenRawText_returnsTrimmedPrompt() {
        ParsedJobRequest parsed = parser.parse("  hello  ");
        assertThat(parsed.prompt()).isEqualTo("hello");
        assertThat(parsed.settings()).isNull();
    }

    @Test
    void parse_whenJsonString_returnsPrompt() {
        ParsedJobRequest parsed = parser.parse("\"hello\"");
        assertThat(parsed.prompt()).isEqualTo("hello");
        assertThat(parsed.settings()).isNull();
    }

    @Test
    void parse_whenJsonObjectWithSettings_returnsPromptAndSettings() {
        ParsedJobRequest parsed = parser.parse("""
                {
                  "prompt": " hello ",
                  "settings": { "steps": 25, "style": "cinematic" }
                }
                """);
        assertThat(parsed.prompt()).isEqualTo("hello");
        assertThat(parsed.settings()).isEqualTo(Map.of("steps", 25, "style", "cinematic"));
    }

    @Test
    void parse_whenJsonObjectMissingPrompt_throwsBusinessException() {
        assertThatThrownBy(() -> parser.parse("{\"settings\":{\"steps\":25}}"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void parse_whenJsonObjectHasNonObjectSettings_ignoresSettings() {
        ParsedJobRequest parsed = parser.parse("""
                {
                  "prompt": "hello",
                  "settings": ["not-an-object"]
                }
                """);
        assertThat(parsed.prompt()).isEqualTo("hello");
        assertThat(parsed.settings()).isNull();
    }

    @Test
    void parse_whenInvalidJson_fallsBackToRawPrompt() {
        ParsedJobRequest parsed = parser.parse("{");
        assertThat(parsed.prompt()).isEqualTo("{");
        assertThat(parsed.settings()).isNull();
    }
}
