package com.planty.repository.crop;

import com.planty.entity.crop.Crop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


// 작물 레포지토리
public interface CropRepository extends JpaRepository<Crop, Integer> {
    // 판매 가능한 작물 목록 불러오기 (판매 게시판)
    List<Crop> findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(Integer userId);
}
