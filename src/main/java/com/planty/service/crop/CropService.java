package com.planty.service.crop;

import com.planty.dto.crop.CropRegistrationDto;
import com.planty.dto.crop.HomeCropDto;
import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import com.planty.repository.diary.DiaryRepository;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final CropRegistrationAnalysisService registrationAnalysisService;
    private final CropDiagnosisAnalysisService diagnosisAnalysisService;
    private final UserRepository userRepository;

    private final DiaryRepository diaryRepository;
 
    /**
     * 작물 이미지 업로드 및 분석 시작 (기존 방식 - 호환성 유지)
     */
    public Crop uploadCropImage(User user, MultipartFile imageFile) throws IOException {
        // 1. 이미지 파일 저장
        String savedImagePath = registrationAnalysisService.saveImageFile(imageFile);

        // 2. 작물 엔티티 생성 (분석 대기 상태)
        Crop crop = new Crop();
        crop.setUser(user);
        crop.setCropImg(savedImagePath);
        crop.setAnalysisStatus(AnalysisStatus.PENDING);
        crop.setIsRegistered(false);
        crop.setHarvest(false);

        Crop savedCrop = cropRepository.save(crop);

        // 3. 비동기로 재배방법 분석 시작
        registrationAnalysisService.analyzeImageAsync(savedCrop.getId(), savedImagePath);

        return savedCrop;
    }

    /**
     * 사용자의 작물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Crop> getUserCrops(User user) {
        return cropRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 홈 화면용 작물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<HomeCropDto> getHomeCrop(Integer userId) {
        List<Crop> crops = cropRepository.findByUser_IdAndHarvestFalseOrderByCreatedAtDesc(userId);
        return crops.stream()
                .map(HomeCropDto::of)
                .map(dto -> {
                    // 이미지 URL을 그대로 사용 (변환 없음)
                    return dto;
                })
                .toList();
    }

    /**
     * 작물 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public Crop getCropById(Integer cropId) {
        return cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));
    }


    /**
     * 새 이미지로 작물 태그별 진단 분석
     */
    public com.planty.dto.crop.CropDetailAnalysisResult analyzeCropDetailWithNewImage(Crop crop, com.planty.entity.crop.AnalysisType analysisType, MultipartFile newImage) throws IOException {
        // 새 이미지 파일 저장
        String savedImagePath = registrationAnalysisService.saveImageFile(newImage);
        log.info("새 이미지 파일 저장 완료: {}", savedImagePath);
        
        // 새 이미지 경로로 임시 Crop 객체 생성하여 진단 분석 수행
        Crop tempCrop = new Crop();
        tempCrop.setId(crop.getId());
        tempCrop.setCropImg(savedImagePath);
        tempCrop.setName(crop.getName());
        tempCrop.setUser(crop.getUser());
        
        log.info("새 이미지로 진단 분석 시작 - 작물 ID: {}, 분석 타입: {}, 새 이미지 경로: {}", 
                crop.getId(), analysisType, savedImagePath);
        
        // 새 이미지로 진단 분석 수행
        return diagnosisAnalysisService.analyzeCropDetail(tempCrop, analysisType);
    }

    /**
     * 새로운 통합 등록 방식: 텍스트 데이터와 이미지를 한 번에 처리하여 재배방법 분석 결과 반환
     */
    public Map<String, Object> analyzeCropWithData(User user, CropRegistrationDto cropData, MultipartFile imageFile) throws IOException {
        return registrationAnalysisService.analyzeCropWithData(user, cropData, imageFile);
    }

    /**
     * 최종 등록: 분석 결과와 텍스트 데이터를 DB에 저장
     */
    public Crop finalizeCropRegistration(User user, Map<String, Object> finalData) {
        try {
            // 임시 작물 ID 추출
            Integer tempCropId = (Integer) finalData.get("tempCropId");
            if (tempCropId == null) {
                throw new IllegalArgumentException("임시 작물 ID가 없습니다.");
            }

            // 임시 작물 조회
            Crop tempCrop = cropRepository.findById(tempCropId)
                    .orElseThrow(() -> new IllegalArgumentException("임시 작물을 찾을 수 없습니다."));

            // 권한 확인
            if (!tempCrop.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("권한이 없습니다.");
            }

            // 분석이 완료되지 않은 경우
            if (tempCrop.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
                throw new IllegalArgumentException("이미지 분석이 완료되지 않았습니다.");
            }

            // 최종 등록 완료 처리
            tempCrop.setIsRegistered(true);
            Crop finalCrop = cropRepository.save(tempCrop);

            log.info("작물 최종 등록 완료: Crop ID {}", finalCrop.getId());

            return finalCrop;

        } catch (Exception e) {
            log.error("최종 등록 중 오류 발생", e);
            throw new RuntimeException("최종 등록에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 작물 재배 완료 상태 변경
     */
    public Crop updateHarvestStatus(Integer cropId, User user, Boolean harvestStatus) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 권한 확인
        if (!crop.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 등록된 작물만 상태 변경 가능
        if (!crop.getIsRegistered()) {
            throw new IllegalArgumentException("등록되지 않은 작물은 상태를 변경할 수 없습니다.");
        }

        crop.setHarvest(harvestStatus);
        Crop updatedCrop = cropRepository.save(crop);

        log.info("작물 재배 완료 상태 변경: Crop ID {}, Harvest Status: {}", cropId, harvestStatus);

        return updatedCrop;
    }

    /**
     * 작물 삭제
     */
    public void deleteCrop(Integer cropId, User user) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 권한 확인
        if (!crop.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // 이미지 파일 삭제
        if (crop.getCropImg() != null) {
            try {
                java.io.File imageFile = new java.io.File(crop.getCropImg());
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            } catch (Exception e) {
                log.warn("이미지 파일 삭제 실패: {}", crop.getCropImg(), e);
            }
        }

        cropRepository.delete(crop);
        log.info("작물 삭제 완료: Crop ID {}", cropId);
    }


    /**
     * 작물 정보 수정 (이미지 포함)
     */
    public Crop updateCropWithImage(Integer cropId, User user, CropRegistrationDto updateDto, MultipartFile imageFile) throws IOException {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 권한 확인
        if (!crop.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        // 등록된 작물만 수정 가능
        if (!crop.getIsRegistered()) {
            throw new IllegalArgumentException("등록되지 않은 작물은 수정할 수 없습니다.");
        }

        // 작물 정보 업데이트
        if (updateDto != null) {
            if (updateDto.getName() != null) {
                crop.setName(updateDto.getName());
            }
            if (updateDto.getStartAt() != null) {
                crop.setStartAt(updateDto.getStartAt());
            }
            if (updateDto.getEndAt() != null) {
                crop.setEndAt(updateDto.getEndAt());
            }
        }

        // 이미지 파일이 있는 경우 이미지 업데이트
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 파일 삭제
            if (crop.getCropImg() != null) {
                try {
                    java.io.File existingImageFile = new java.io.File(crop.getCropImg());
                    if (existingImageFile.exists()) {
                        existingImageFile.delete();
                        log.info("기존 이미지 파일 삭제: {}", crop.getCropImg());
                    }
                } catch (Exception e) {
                    log.warn("기존 이미지 파일 삭제 실패: {}", crop.getCropImg(), e);
                }
            }

            // 새 이미지 파일 저장
            String savedImagePath = registrationAnalysisService.saveImageFile(imageFile);
            crop.setCropImg(savedImagePath);
            log.info("새 이미지 파일 저장: {}", savedImagePath);
        }

        Crop updatedCrop = cropRepository.save(crop);
        log.info("작물 정보 수정 완료 (이미지 포함): Crop ID {}", cropId);

        return updatedCrop;
    }

    /**
     * 작물과 관련된 재배일지 목록 조회 (직접 작물 ID로 조회)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCropDiariesByCategory(Integer cropId, Integer userId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 해당 작물 ID로 직접 재배일지 조회 (카테고리 필터링 제거)
        List<Diary> diaries = diaryRepository.findByCropIdOrderByCreatedAtDesc(cropId);
        
        log.info("작물 ID {}에 대한 재배일지 수: {}", cropId, diaries.size());

        return diaries.stream()
                .map(diary -> {
                    Map<String, Object> diaryInfo = new LinkedHashMap<>();
                    diaryInfo.put("diaryId", diary.getId());
                    diaryInfo.put("title", diary.getTitle());
                    diaryInfo.put("content", diary.getContent());
                    diaryInfo.put("cropName", diary.getCrop() != null ? diary.getCrop().getName() : "알 수 없음");
                    diaryInfo.put("createdAt", diary.getCreatedAt());
                    diaryInfo.put("modifiedAt", diary.getModifiedAt());
                    
                    // 썸네일 이미지 찾기
                    String thumbnailImage = diary.getImages().stream()
                            .filter(DiaryImage::getThumbnail)
                            .map(DiaryImage::getDiaryImg)
                            .findFirst()
                            .orElse(null);
                    diaryInfo.put("thumbnailImage", thumbnailImage);
                    
                    // 전체 이미지 목록
                    List<String> images = diary.getImages().stream()
                            .map(DiaryImage::getDiaryImg)
                            .toList();
                    diaryInfo.put("images", images);
                    
                    return diaryInfo;
                })
                .toList();
    }
}

