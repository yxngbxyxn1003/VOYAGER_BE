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
     * 작물 기본 정보로 임시 등록 (이미지 업로드 전)
     */
    public Crop createTempCrop(User user, CropRegistrationDto dto) {
        Crop crop = new Crop();
        crop.setUser(user);
        crop.setName(dto.getName());
        crop.setStartAt(dto.getStartAt());
        crop.setEndAt(dto.getEndAt());
        crop.setAnalysisStatus(AnalysisStatus.PENDING);
        crop.setIsRegistered(false);
        crop.setHarvest(false);

        return cropRepository.save(crop);
    }

    /**
     * 임시 등록된 작물에 이미지 업로드 및 AI 분석 시작
     */
    public Crop uploadCropImageToExisting(Integer cropId, MultipartFile imageFile) throws IOException {
        // 1. 기존 작물 조회
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));

        // 2. 이미지 파일 저장 및 분석 시작 (재배방법 분석 서비스 사용)
        String savedImagePath = registrationAnalysisService.saveImageFile(imageFile);
        crop.setCropImg(savedImagePath);
        crop.setAnalysisStatus(AnalysisStatus.ANALYZING);

        Crop savedCrop = cropRepository.save(crop);

        // 3. 비동기로 재배방법 분석 시작
        registrationAnalysisService.analyzeImageAsync(savedCrop.getId(), savedImagePath);

        return savedCrop;
    }

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
//
//    /**
//     * 작물 등록 완료 (이름, 날짜 등 입력 후)
//     */
//    public Crop completeCropRegistration(Integer cropId, CropRegistrationDto dto) {
//        Crop crop = cropRepository.findById(cropId)
//                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));
//
//        // 분석이 완료된 경우에만 등록 가능
//        if (crop.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
//            throw new IllegalStateException("분석이 완료되지 않은 작물은 등록할 수 없습니다.");
//        }
//
//        // 사용자 입력 정보 업데이트
//        crop.setName(dto.getName());
//        crop.setStartAt(dto.getStartAt());
//        crop.setEndAt(dto.getEndAt());
//        crop.setIsRegistered(true); // 등록 완료
//
//        return cropRepository.save(crop);
//    }
//
    /**
     * 사용자의 작물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Crop> getUserCrops(User user) {
        return cropRepository.findByUserOrderByCreatedAtDesc(user);
    }
//
//    /**
//     * 사용자의 등록된 작물 목록 조회
//     */
//    @Transactional(readOnly = true)
//    public List<Crop> getUserRegisteredCrops(User user) {
//        return cropRepository.findByUserAndIsRegisteredTrueOrderByCreatedAtDesc(user);
//    }
//
    /**
     * 작물 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public Crop getCropById(Integer cropId) {
        return cropRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));
    }
//

//
//    /**
//     * 작물 삭제
//     */
//    public void deleteCrop(Integer cropId, User user) {
//        Crop crop = cropRepository.findById(cropId)
//                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));
//
//        // 권한 확인
//        if (!crop.getUser().getId().equals(user.getId())) {
//            throw new IllegalArgumentException("삭제 권한이 없습니다.");
//        }
//
//        // 이미지 파일 삭제
//        if (crop.getCropImg() != null) {
//            try {
//                File imageFile = new File(crop.getCropImg());
//                if (imageFile.exists()) {
//                    imageFile.delete();
//                }
//            } catch (Exception e) {
//                log.warn("이미지 파일 삭제 실패: {}", crop.getCropImg(), e);
//            }
//        }
//
//        cropRepository.delete(crop);
//    }
//
//    /**
//     * 홈 화면용 사용자 작물 목록 조회 (등록된 것과 미등록된 것 모두)
//     */
//    @Transactional(readOnly = true)
//    public List<HomeCropDto> getHomeCrops(User user) {
//        List<Crop> crops = cropRepository.findByUserOrderByCreatedAtDesc(user);
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
//
//        return crops.stream()
//                .map(crop -> {
//                    HomeCropDto dto = new HomeCropDto();
//                    dto.setId(crop.getId());
//                    dto.setName(crop.getName() != null ? crop.getName() : "분석 중인 작물");
//                    dto.setCropImg(crop.getCropImg());
//                    dto.setPlantingDate(crop.getStartAt() != null ?
//                        crop.getStartAt().format(formatter) :
//                        "재배 시작일 미입력");
//                    dto.setIsRegistered(crop.getIsRegistered());
//                    dto.setAnalysisStatus(crop.getAnalysisStatus().toString());
//                    dto.setCropCategory(crop.getCropCategory() != null ?
//                        crop.getCropCategory().toString() : "미분류");
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
//
    /**
     * 작물 태그별 진단 분석 (현재상태, 질병여부, 품질/시장성)
     */
    public com.planty.dto.crop.CropDetailAnalysisResult analyzeCropDetail(Crop crop, com.planty.entity.crop.AnalysisType analysisType) {
        return diagnosisAnalysisService.analyzeCropDetail(crop, analysisType);
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
