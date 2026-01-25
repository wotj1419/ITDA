package com.itda.backend.worker.image;

import com.itda.backend.worker.LocalFileStorage;
import com.itda.backend.worker.LocalJobAssetStorage;
import org.springframework.stereotype.Component;

@Component
public class LocalImageStorage extends LocalJobAssetStorage {

    public LocalImageStorage(LocalFileStorage localFileStorage) {
        super(localFileStorage, "image", "ai/images", ".png");
    }
}
