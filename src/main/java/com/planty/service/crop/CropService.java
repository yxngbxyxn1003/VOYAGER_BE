package com.planty.service.crop;

import com.planty.dto.crop.CropAnalysisResult;
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.dto.crop.CropRegistrationDto;
import com.planty.dto.crop.HomeCropDto;
import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.service.openai.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CropService {
//
//    private final CropRepository cropRepository;
//    private final OpenAIService openAIService;
//
//    @Value("${app.upload.path:uploads/}")
//    private String uploadPath;
//
//    /**
//     * 작물 이미지 업로드 및 분석 시작
//     */
//    public Crop uploadCropImage(User user, MultipartFile imageFile) throws IOException {
//        // 1. 이미지 파일 저장
//        String savedImagePath = saveImageFile(imageFile);
//
//        // 2. 작물 엔티티 생성 (분석 대기 상태)
//        Crop crop = new Crop();
//        crop.setUser(user);
//        crop.setCropImg(savedImagePath);
//        crop.setAnalysisStatus(AnalysisStatus.PENDING);
//        crop.setIsRegistered(false);
//        crop.setHarvest(false);
//
//        Crop savedCrop = cropRepository.save(crop);
//
//        // 3. 비동기로 이미지 분석 시작
//        analyzeImageAsync(savedCrop.getId(), savedImagePath);
//
//        return savedCrop;
//    }
//
//    /**
//     * 비동기 이미지 분석
//     */
//    @Transactional
//    public void analyzeImageAsync(Integer cropId, String imagePath) {
//        CompletableFuture.runAsync(() -> {
//            try {
//                // 분석 상태를 ANALYZING으로 변경
//                Crop crop = cropRepository.findById(cropId).orElse(null);
//                if (crop == null) {
//                    log.error("작물을 찾을 수 없습니다. ID: {}", cropId);
//                    return;
//                }
//
//                crop.setAnalysisStatus(AnalysisStatus.ANALYZING);
//                cropRepository.save(crop);
//
//                // OpenAI로 이미지 분석
//                CropAnalysisResult analysisResult = openAIService.analyzeCropImage(imagePath);
//
//                // 분석 결과로 작물 정보 업데이트
//                if (analysisResult.isSuccess()) {
//                    crop.setAnalysisStatus(AnalysisStatus.COMPLETED);
//                    crop.setEnvironment(analysisResult.getEnvironment());
//                    crop.setTemperature(analysisResult.getTemperature());
//                    crop.setHeight(analysisResult.getHeight());
//                    crop.setHowTo(analysisResult.getHowTo());
//                    // 분석 완료 후에도 isRegistered는 false로 유지 (사용자가 직접 등록 완료해야 함)
//                } else {
//                    crop.setAnalysisStatus(AnalysisStatus.FAILED);
//                    log.error("이미지 분석 실패: {}", analysisResult.getAnalysisMessage());
//                }
//
//                cropRepository.save(crop);
//
//            } catch (Exception e) {
//                log.error("이미지 분석 중 오류 발생", e);
//                // 실패 상태로 업데이트
//                Crop crop = cropRepository.findById(cropId).orElse(null);
//                if (crop != null) {
//                    crop.setAnalysisStatus(AnalysisStatus.FAILED);
//                    cropRepository.save(crop);
//                }
//            }
//        });
//    }
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
//    /**
//     * 사용자의 작물 목록 조회
//     */
//    @Transactional(readOnly = true)
//    public List<Crop> getUserCrops(User user) {
//        return cropRepository.findByUserOrderByCreatedAtDesc(user);
//    }
//
//    /**
//     * 사용자의 등록된 작물 목록 조회
//     */
//    @Transactional(readOnly = true)
//    public List<Crop> getUserRegisteredCrops(User user) {
//        return cropRepository.findByUserAndIsRegisteredTrueOrderByCreatedAtDesc(user);
//    }
//
//    /**
//     * 작물 상세 정보 조회
//     */
//    @Transactional(readOnly = true)
//    public Crop getCropById(Integer cropId) {
//        return cropRepository.findById(cropId)
//                .orElseThrow(() -> new IllegalArgumentException("작물을 찾을 수 없습니다."));
//    }
//
//    /**
//     * 이미지 파일 저장
//     */
//    private String saveImageFile(MultipartFile file) throws IOException {
//        if (file.isEmpty()) {
//            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
//        }
//
//        // 업로드 디렉토리 생성
//        File uploadDir = new File(uploadPath);
//        if (!uploadDir.exists()) {
//            uploadDir.mkdirs();
//        }
//
//        // 고유한 파일명 생성
//        String originalFilename = file.getOriginalFilename();
//        String extension = "";
//        if (originalFilename != null && originalFilename.contains(".")) {
//            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
//        }
//        String fileName = UUID.randomUUID().toString() + extension;
//
//        // 파일 저장
//        File destinationFile = new File(uploadDir, fileName);
//        file.transferTo(destinationFile);
//
//        return destinationFile.getAbsolutePath();
//    }
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
//    /**
//     * 작물 세부 분석 (현재상태, 질병여부, 품질/시장성)
//     */
//    public CropDetailAnalysisResult analyzeCropDetail(Crop crop, AnalysisType analysisType) {
//        try {
//            // 작물 이미지가 없는 경우
//            if (crop.getCropImg() == null || crop.getCropImg().trim().isEmpty()) {
//                return new CropDetailAnalysisResult(false, "분석할 이미지가 없습니다.", analysisType);
//            }
//
//            // 이미지 파일 존재 확인
//            File imageFile = new File(crop.getCropImg());
//            if (!imageFile.exists()) {
//                return new CropDetailAnalysisResult(false, "이미지 파일을 찾을 수 없습니다.", analysisType);
//            }
//
//            // OpenAI를 통한 세부 분석
//            CropDetailAnalysisResult result = openAIService.analyzeCropDetail(crop.getCropImg(), analysisType);
//
//            log.info("작물 세부 분석 완료 - 작물 ID: {}, 분석 타입: {}, 성공: {}",
//                    crop.getId(), analysisType, result.isSuccess());
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("작물 세부 분석 중 오류 발생 - 작물 ID: {}, 분석 타입: {}", crop.getId(), analysisType, e);
//            return new CropDetailAnalysisResult(false, "분석 중 오류가 발생했습니다: " + e.getMessage(), analysisType);
//        }
//    }
}
