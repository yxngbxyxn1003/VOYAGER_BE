package com.planty.repository.diary;

import com.planty.entity.diary.Diary;
import com.planty.entity.user.User;
import com.planty.entity.crop.Crop;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


// 재배일지 Repository
public interface DiaryRepository extends JpaRepository<Diary, Integer> {
    
    // 재배일지 상세 조회 (연관 엔티티 함께 로드)
    @EntityGraph(attributePaths = {"user", "crop", "images"})
    Optional<Diary> findById(Integer id);
    
    
    
    // 작물별 재배일지 목록 조회 (최신순) - crop이 null인 경우는 제외
    @EntityGraph(attributePaths = {"user", "crop", "images"})
    List<Diary> findByCropIdOrderByCreatedAtDesc(Integer cropId);
    
   
    
    // 카테고리 기반으로 재배일지 검색 (JPQL 사용) - crop이 null인 경우는 제외
    @Query("SELECT d FROM Diary d " +
           "JOIN d.crop c " +
           "JOIN c.categories cat " +
           "WHERE d.user = :user " +
           "AND cat.categoryName IN :categoryNames " +
           "ORDER BY d.createdAt DESC")
    List<Diary> findByUserAndCropCategoriesOrderByCreatedAtDesc(
        @Param("user") User user, 
        @Param("categoryNames") List<String> categoryNames
    );
    
    // 사용자별로 특정 작물 이름의 재배일지 검색 - crop이 null인 경우는 제외
    @EntityGraph(attributePaths = {"crop", "images"})
    List<Diary> findByUserAndCrop_NameOrderByCreatedAtDesc(User user, String cropName);
    
    // 사용자별 진단결과 기반 재배일지 목록 조회 (crop이 null인 경우만)
    @EntityGraph(attributePaths = {"images"})
    List<Diary> findByUserAndCropIsNullOrderByCreatedAtDesc(User user);
    
    // 사용자별 일반 재배일지 목록 조회 (crop이 null이 아닌 경우만)
    @EntityGraph(attributePaths = {"crop", "images"})
    List<Diary> findByUserAndCropIsNotNullOrderByCreatedAtDesc(User user);
    
    // 사용자와 특정 작물의 재배일지 목록 조회
    @EntityGraph(attributePaths = {"crop", "images"})
    List<Diary> findByUserAndCropOrderByCreatedAtDesc(User user, Crop crop);

    
    List<Diary> findByUserOrderByCreatedAtDesc(User user);
}


