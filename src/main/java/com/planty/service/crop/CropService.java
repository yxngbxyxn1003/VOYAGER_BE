package com.planty.service.crop;

import com.planty.dto.crop.CropRegistrationDto;
import com.planty.dto.crop.HomeCropDto;
import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.crop.CropCategoryRepository;
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
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.CropCategory;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final CropCategoryRepository cropCategoryRepository;
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
    @Transactional
    public List<HomeCropDto> getHomeCrop(Integer userId) {
        List<Crop> crops = cropRepository.findByUser_IdAndHarvestFalseOrderByCreatedAtDesc(userId);
        
        // 카테고리 정보를 명시적으로 로드하고 없으면 생성
        crops.forEach(crop -> {
            if (crop.getCategories() == null || crop.getCategories().isEmpty()) {
                // 작물 이름에 따라 적절한 카테고리 설정
                String categoryName = getCategoryByCropName(crop.getName());
                
                // 새로운 카테고리 생성
                CropCategory defaultCategory = new CropCategory();
                defaultCategory.setCategoryName(categoryName);
                defaultCategory.setCrop(crop);
                
                // 카테고리를 직접 저장
                cropCategoryRepository.save(defaultCategory);
                
                // 엔티티 상태 새로고침
                cropRepository.flush();
            }
        });
        
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
     * 작물 상세 정보 조회 (사용자 정보 포함)
     */
    @Transactional(readOnly = true)
    public Crop getCropByIdWithUser(Integer cropId) {
        return cropRepository.findByIdWithUser(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));
    }


    /**
     * 작물 상세페이지에서 진단받기 (해당 cropID로 진단 진행)
     */
    public CropDetailAnalysisResult analyzeCropDiagnosis(Integer cropId, User user, AnalysisType analysisType, MultipartFile image) throws IOException {
        return diagnosisAnalysisService.analyzeCropDiagnosis(cropId, user, analysisType, image);
    }

    /**
     작물 상세페이지에서 cropID 기반 진단을 사용
     */
    @Deprecated
    public CropDetailAnalysisResult analyzeCropDiagnosisStandalone(User user, AnalysisType analysisType, MultipartFile image) throws IOException {
        return diagnosisAnalysisService.analyzeCropDiagnosisStandalone(user, analysisType, image);
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
     * 작물 삭제 (연결된 재배일지도 함께 삭제)
     */
    public void deleteCrop(Integer cropId, User user) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 권한 확인
        if (!crop.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // 연결된 재배일지 먼저 삭제
        List<Diary> connectedDiaries = diaryRepository.findByCropIdOrderByCreatedAtDesc(cropId);
        if (!connectedDiaries.isEmpty()) {
            log.info("작물 ID {}에 연결된 재배일지 {}개 삭제 시작", cropId, connectedDiaries.size());
            
            for (Diary diary : connectedDiaries) {
                // 재배일지 이미지 파일들 삭제
                for (DiaryImage diaryImage : diary.getImages()) {
                    try {
                        java.io.File imageFile = new java.io.File(diaryImage.getDiaryImg());
                        if (imageFile.exists()) {
                            imageFile.delete();
                            log.info("재배일지 이미지 파일 삭제: {}", diaryImage.getDiaryImg());
                        }
                    } catch (Exception e) {
                        log.warn("재배일지 이미지 파일 삭제 실패: {}", diaryImage.getDiaryImg(), e);
                    }
                }
            }
            
            // 재배일지 엔티티들 삭제
            diaryRepository.deleteAll(connectedDiaries);
            log.info("작물 ID {}에 연결된 재배일지 {}개 삭제 완료", cropId, connectedDiaries.size());
        }

        // 작물 이미지 파일 삭제
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

        // 작물 엔티티 삭제
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
     * 해당 작물 ID에 직접 연결된 재배일지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCropDiaries(Integer cropId, Integer userId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 해당 작물 ID에 직접 연결된 재배일지 검색
        List<Diary> diaries = diaryRepository.findByCropIdOrderByCreatedAtDesc(cropId);

        return diaries.stream()
                .map(diary -> {
                    Map<String, Object> diaryInfo = new LinkedHashMap<>();
                    diaryInfo.put("diaryId", diary.getId());
                    diaryInfo.put("title", diary.getTitle());
                    diaryInfo.put("content", diary.getContent());
                    diaryInfo.put("cropName", diary.getCrop().getName());
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
    
    /**
     * 작물 이름에 따라 7개 대분류 카테고리를 반환하는 메서드
     */
    private String getCategoryByCropName(String cropName) {
        if (cropName == null) return "기타";
        
        String name = cropName.toLowerCase();
        
        // 잎채소
        if (name.contains("상추") || name.contains("lettuce")) return "잎채소";
        if (name.contains("배추") || name.contains("cabbage")) return "잎채소";
        if (name.contains("양배추") || name.contains("cabbage")) return "잎채소";
        if (name.contains("시금치") || name.contains("spinach")) return "잎채소";
        if (name.contains("케일") || name.contains("kale")) return "잎채소";
        if (name.contains("청경채") || name.contains("bok choy")) return "잎채소";
        if (name.contains("근대") || name.contains("chard")) return "잎채소";
        if (name.contains("갓") || name.contains("mustard")) return "잎채소";
        
        // 과채
        if (name.contains("토마토") || name.contains("tomato")) return "과채";
        if (name.contains("오이") || name.contains("cucumber")) return "과채";
        if (name.contains("고추") || name.contains("pepper")) return "과채";
        if (name.contains("가지") || name.contains("eggplant")) return "과채";
        if (name.contains("호박") || name.contains("pumpkin")) return "과채";
        if (name.contains("수박") || name.contains("watermelon")) return "과채";
        if (name.contains("참외") || name.contains("melon")) return "과채";
        if (name.contains("멜론") || name.contains("cantaloupe")) return "과채";
        
        // 뿌리채소
        if (name.contains("당근") || name.contains("carrot")) return "뿌리채소";
        if (name.contains("무") || name.contains("radish")) return "뿌리채소";
        if (name.contains("감자") || name.contains("potato")) return "뿌리채소";
        if (name.contains("고구마") || name.contains("sweet potato")) return "뿌리채소";
        if (name.contains("양파") || name.contains("onion")) return "뿌리채소";
        if (name.contains("마늘") || name.contains("garlic")) return "뿌리채소";
        if (name.contains("생강") || name.contains("ginger")) return "뿌리채소";
        if (name.contains("우엉") || name.contains("burdock")) return "뿌리채소";
        if (name.contains("연근") || name.contains("lotus root")) return "뿌리채소";
        if (name.contains("비트") || name.contains("beet")) return "뿌리채소";
        
        // 십자화과 채소
        if (name.contains("브로콜리") || name.contains("broccoli")) return "십자화과 채소";
        if (name.contains("콜리플라워") || name.contains("cauliflower")) return "십자화과 채소";
        
        // 콩
        if (name.contains("콩") || name.contains("bean")) return "콩";
        if (name.contains("팥") || name.contains("red bean")) return "콩";
        if (name.contains("녹두") || name.contains("mung bean")) return "콩";
        if (name.contains("완두") || name.contains("pea")) return "콩";
        if (name.contains("강낭콩") || name.contains("kidney bean")) return "콩";
        if (name.contains("병아리콩") || name.contains("chickpea")) return "콩";
        
        // 허브
        if (name.contains("바질") || name.contains("basil")) return "허브";
        if (name.contains("로즈마리") || name.contains("rosemary")) return "허브";
        if (name.contains("타임") || name.contains("thyme")) return "허브";
        if (name.contains("민트") || name.contains("mint")) return "허브";
        if (name.contains("파슬리") || name.contains("parsley")) return "허브";
        if (name.contains("세이지") || name.contains("sage")) return "허브";
        if (name.contains("오레가노") || name.contains("oregano")) return "허브";
        if (name.contains("라벤더") || name.contains("lavender")) return "허브";
        if (name.contains("딜") || name.contains("dill")) return "허브";
        if (name.contains("코리앤더") || name.contains("coriander")) return "허브";
        
        // 기본값
        return "기타";
    }
}

