package com.itda.backend.ai.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiSettingsLogger implements ApplicationRunner {

    private final GeminiProperties geminiProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[GeminiSettings] stub={}, model={}, responseModalities={}, responseMimeType={}",
                geminiProperties.isStub(),
                geminiProperties.getImageModel(),
                geminiProperties.getResponseModalities(),
                geminiProperties.getResponseMimeType());
    }
}
