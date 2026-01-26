package com.itda.backend.ai;

public class AiProviderException extends RuntimeException {

    private final String summary;

    public AiProviderException(String summary) {
        super(summary);
        this.summary = summary;
    }

    public AiProviderException(String summary, Throwable cause) {
        super(summary, cause);
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }
}
