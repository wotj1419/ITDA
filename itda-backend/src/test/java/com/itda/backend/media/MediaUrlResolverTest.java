package com.itda.backend.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaUrlResolverTest {

    private final MediaUrlResolver resolver = new MediaUrlResolver();

    @Test
    void returnsAbsoluteUrlWhenProvided() {
        String contentUrl = "https://example.com/media/abc.png";

        String result = resolver.nodeContentUrl(null, contentUrl);

        assertThat(result).isEqualTo(contentUrl);
    }

    @Test
    void buildsInternalUrlWhenNodeIdPresentAndContentUrlMissing() {
        String result = resolver.nodeContentUrl(42L, null);

        assertThat(result).isEqualTo("/api/nodes/42/content");
    }

    @Test
    void buildsInternalUrlWhenContentUrlIsRelative() {
        String result = resolver.nodeContentUrl(7L, "uploads/7.png");

        assertThat(result).isEqualTo("/api/nodes/7/content");
    }

    @Test
    void returnsNullWhenNoNodeIdAndNoContentUrl() {
        String result = resolver.nodeContentUrl(null, null);

        assertThat(result).isNull();
    }
}
