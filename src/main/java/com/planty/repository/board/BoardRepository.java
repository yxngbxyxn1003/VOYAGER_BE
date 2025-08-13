package com.planty.repository.board;

import com.planty.entity.crop.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


// 판매 게시판 레포지토리
@Repository
public interface BoardRepository extends JpaRepository<Crop, Integer> {
    List<Crop> findByUserIdAndHarvestTrueOrderByCreatedAtDesc(Integer userId);
}

