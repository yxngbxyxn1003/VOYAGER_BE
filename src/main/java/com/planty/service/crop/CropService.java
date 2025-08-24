package com.planty.service.crop;

import com.planty.dto.crop.CropRegistrationDto;
import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final CropRegistrationAnalysisService registrationAnalysisService;
    private final CropDiagnosisAnalysisService diagnosisAnalysisService;
    
    /**
     * 통합 작물 등록: 작물 정보와 이미지를 한 번에 입력받아 AI 재배방법 분석 시작
     * 사용자가 작물 이름, 재배 시작/종료 날짜, 이미지를 모두 입력하면 즉시 분석을 시작합니다.
     */
    public Crop registerCropWithImage(User user, CropRegistrationDto dto) throws IOException {
        log.info("CropService.registerCropWithImage 시작 - 사용자: {}, 파일: {}", 
                user.getId(), dto.getImageFile() != null ? dto.getImageFile().getOriginalFilename() : "이미지 없음");
        
        // 1. 이미지 파일 저장 (이미지가 null만 아니면 모든 이미지 허용)
        String savedImagePath = null;
        if (dto.getImageFile() != null) {
            savedImagePath = registrationAnalysisService.saveImageFile(dto.getImageFile());
            log.info("이미지 파일 저장 완료: {}", savedImagePath);
        } else {
            log.info("이미지 파일이 null임");
        }
        
        // 2. 작물 엔티티 생성 및 저장 (분석 중 상태로 시작)
        Crop crop = new Crop();
        crop.setUser(user);
        crop.setName(dto.getName());
        crop.setStartAt(dto.getStartAt());
        crop.setEndAt(dto.getEndAt());
        crop.setCropImg(savedImagePath);
        crop.setAnalysisStatus(AnalysisStatus.ANALYZING);
        crop.setIsRegistered(false);
        crop.setHarvest(false);
        
        Crop savedCrop = cropRepository.save(crop);
        
        // 3. 이미지가 있는 경우에만 AI 분석 시작
        if (savedImagePath != null) {
            registrationAnalysisService.analyzeImageAsync(savedCrop.getId(), savedImagePath);
            log.info("작물 등록 및 AI 분석 시작: Crop ID {}, Name: {}, 분석 상태: {}", 
                    savedCrop.getId(), savedCrop.getName(), savedCrop.getAnalysisStatus());
        } else {
            log.info("작물 등록 완료 (이미지 없음): Crop ID {}, Name: {}", 
                    savedCrop.getId(), savedCrop.getName());
        }
        
        return savedCrop;
    }

    /**
     * 사용자의 작물 목록 조회 (엔티티 직접 반환)
     */
    @Transactional(readOnly = true)
    public List<Crop> getUserCrops(User user) {
        return cropRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 홈 화면용 사용자 작물 목록 조회 (DTO 변환하여 반환)
     */
    @Transactional(readOnly = true)
    public List<com.planty.dto.crop.HomeCropDto> getHomeCrops(User user) {
        return getUserCrops(user).stream()
                .map(this::convertToHomeCropDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Crop 엔티티를 HomeCropDto로 변환
     */
    private com.planty.dto.crop.HomeCropDto convertToHomeCropDto(Crop crop) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 카테고리명 추출 (안전하게 처리)
        String categoryName = "기타";
        try {
            if (crop.getCategories() != null && !crop.getCategories().isEmpty()) {
                categoryName = crop.getCategories().get(0).getCategoryName();
            }
        } catch (Exception e) {
            log.warn("카테고리 정보 추출 실패 - Crop ID: {}, Error: {}", crop.getId(), e.getMessage());
            categoryName = "기타";
        }
        
        return new com.planty.dto.crop.HomeCropDto(
                crop.getId(),
                crop.getName(),
                crop.getCropImg(),
                crop.getStartAt() != null ? crop.getStartAt().format(formatter) : null,
                crop.getEndAt() != null ? crop.getEndAt().format(formatter) : null,
                crop.getIsRegistered(),
                crop.getAnalysisStatus() != null ? crop.getAnalysisStatus().name() : null,
                categoryName
        );
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
     * 작물 태그별 진단 분석 (현재상태, 질병여부, 품질/시장성)
     */
    public com.planty.dto.crop.CropDetailAnalysisResult analyzeCropDetail(Crop crop, com.planty.entity.crop.AnalysisType analysisType) {
        return diagnosisAnalysisService.analyzeCropDetail(crop, analysisType);
    }



    /**
     * AI 분석 완료 후 작물 등록 완료 처리
     * 분석이 완료된 작물을 최종 등록 상태로 변경합니다.
     */
    public Crop completeCropRegistration(Integer cropId, User user) {
        try {
            Crop crop = cropRepository.findById(cropId)
                    .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

            // 권한 확인
            if (!crop.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("권한이 없습니다.");
            }

            // 분석이 완료되지 않은 경우
            if (crop.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
                throw new IllegalArgumentException("AI 분석이 완료되지 않았습니다. 잠시 후 다시 시도해주세요.");
            }

            // 최종 등록 완료 처리
            crop.setIsRegistered(true);
            Crop finalCrop = cropRepository.save(crop);

            log.info("작물 등록 완료: Crop ID {}, Name: {}", finalCrop.getId(), finalCrop.getName());

            return finalCrop;

        } catch (Exception e) {
            log.error("작물 등록 완료 처리 중 오류 발생", e);
            throw new RuntimeException("작물 등록 완료에 실패했습니다: " + e.getMessage());
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
     * 작물 정보 수정
     */
    public Crop updateCrop(Integer cropId, User user, CropRegistrationDto updateDto) {
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
        if (updateDto.getName() != null) {
            crop.setName(updateDto.getName());
        }
        if (updateDto.getStartAt() != null) {
            crop.setStartAt(updateDto.getStartAt());
        }
        if (updateDto.getEndAt() != null) {
            crop.setEndAt(updateDto.getEndAt());
        }

        Crop updatedCrop = cropRepository.save(crop);
        log.info("작물 정보 수정 완료: Crop ID {}", cropId);

        return updatedCrop;
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
}
