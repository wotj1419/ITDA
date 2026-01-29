package com.itda.backend.worker.video;

import com.itda.backend.asset.domain.Asset;

import java.nio.file.Path;

public interface VideoStorage {

    Asset storeMergedVideo(Path localFile, String storageKey);
}
