package com.itda.backend.worker;

public record NodeContent(byte[] bytes, String contentType, String contentKey) {
}
