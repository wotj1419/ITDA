package com.itda.backend.scenario.controller.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KeywordsDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        List<String> keywords = new ArrayList<>();

        if (node == null || node.isNull()) {
            return keywords;
        }

        if (node.isTextual()) {
            String text = node.asText();
            if (!text.isBlank()) {
                for (String token : text.split(",")) {
                    String trimmed = token.trim();
                    if (!trimmed.isEmpty()) {
                        keywords.add(trimmed);
                    }
                }
            }
            return keywords;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    String trimmed = item.asText().trim();
                    if (!trimmed.isEmpty()) {
                        keywords.add(trimmed);
                    }
                } else if (!item.isNull()) {
                    throw new IllegalArgumentException("keywords must be a string or array of strings");
                }
            }
            return keywords;
        }

        throw new IllegalArgumentException("keywords must be a string or array of strings");
    }
}
