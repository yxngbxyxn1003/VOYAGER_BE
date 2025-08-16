package com.planty.repository.crop;

import com.planty.entity.crop.Crop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


// 작물 레포지토리
public interface CropRepository extends JpaRepository<Crop, Integer> {
    // 재배 완료된 작물 불러오기
    List<Crop> findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(Integer userId);
}
