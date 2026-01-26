package com.itda.backend.worker.video;

import com.itda.backend.worker.LocalFileStorage;
import com.itda.backend.worker.LocalJobAssetStorage;
import org.springframework.stereotype.Component;

@Component
public class LocalVideoStorage extends LocalJobAssetStorage {

    public LocalVideoStorage(LocalFileStorage localFileStorage) {
        super(localFileStorage, "video", "ai/videos", ".mp4");
    }
}
