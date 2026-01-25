package com.itda.backend.asset.repository;

import com.itda.backend.asset.domain.Asset;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;
@Mapper
public interface AssetMapper {

    void insert(Asset asset);

    Optional<Asset> findById(Long id);
}
