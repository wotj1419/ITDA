package com.itda.backend.worker.video;

import java.nio.file.Path;

public record VideoInput(Path path, boolean temporary) {
}
