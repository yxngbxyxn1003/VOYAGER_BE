package com.planty.repository.crop;

import com.planty.entity.crop.Crop;
import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CropRepository extends JpaRepository<Crop, Integer> {
    
    // 사용자별 작물 목록 조회
    List<Crop> findByUserOrderByCreatedAtDesc(User user);
    
    // 사용자별 등록된 작물 목록 조회 (카테고리 정보 포함)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"categories"})
    List<Crop> findByUserAndIsRegisteredTrueOrderByCreatedAtDesc(User user);
    
    // 홈 화면용 작물 목록 조회 (수확하지 않은 작물만)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"categories"})
    List<Crop> findByUser_IdAndHarvestFalseOrderByCreatedAtDesc(Integer userId);
    
    // 분석 상태별 작물 조회
    List<Crop> findByAnalysisStatus(AnalysisStatus status);
    
    // 사용자의 특정 분석 상태 작물 조회
    List<Crop> findByUserAndAnalysisStatus(User user, AnalysisStatus status);
    
    // 사용자의 최근 등록된 작물 조회
    @Query("SELECT c FROM Crop c WHERE c.user = :user AND c.isRegistered = true ORDER BY c.createdAt DESC")
    List<Crop> findRecentRegisteredCropsByUser(@Param("user") User user);

    // 재배 완료된 작물 불러오기
    List<Crop> findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(Integer userId);

}