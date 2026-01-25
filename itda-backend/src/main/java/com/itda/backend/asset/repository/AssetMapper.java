package com.itda.backend.asset.repository;

import com.itda.backend.asset.domain.Asset;
import org.apache.ibatis.annotations.Mapper;

/**
 * Asset MyBatis Mapper
 */
@Mapper
public interface AssetMapper {

    void insert(Asset asset);
}
