package com.planty.repository.crop;

import com.planty.entity.crop.CropCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropCategoryRepository extends JpaRepository<CropCategory, Integer> {
}
