package com.planty.service.crop;

import com.planty.dto.crop.CropAnalysisResult;
import com.planty.dto.crop.CropRegistrationDto;
import com.planty.entity.crop.AnalysisStatus;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.service.openai.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 작물 등록 시 재배방법 분석 전용 서비스
 * - 작물 등록 시 이미지를 분석하여 재배방법, 환경, 온도, 높이 등 기본 정보 제공
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CropRegistrationAnalysisService {

    private final CropRepository cropRepository;
    private final OpenAIService openAIService;

    /**
     * 비동기 재배방법 분석 (작물 등록 시)
     */
    @Transactional
    public void analyzeImageAsync(Integer cropId, String imagePath) {
        CompletableFuture.runAsync(() -> {
            try {
                // 분석 상태를 ANALYZING으로 변경
                Crop crop = cropRepository.findById(cropId).orElse(null);
                if (crop == null) {
                    log.error("작물을 찾을 수 없습니다. ID: {}", cropId);
                    return;
                }

                crop.setAnalysisStatus(AnalysisStatus.ANALYZING);
                cropRepository.save(crop);

                // OpenAI로 재배방법 분석 (REGISTRATION_ANALYSIS)
                CropAnalysisResult analysisResult = openAIService.analyzeCropImage(imagePath, com.planty.entity.crop.AnalysisType.REGISTRATION_ANALYSIS);

                // 분석 결과로 작물 정보 업데이트
                if (analysisResult.isSuccess()) {
                    crop.setAnalysisStatus(AnalysisStatus.COMPLETED);
                    crop.setEnvironment(analysisResult.getEnvironment());
                    crop.setTemperature(analysisResult.getTemperature());
                    crop.setHeight(analysisResult.getHeight());
                    crop.setHowTo(analysisResult.getHowTo());
                    // AI 분석 완료 시 자동으로 등록 완료 상태로 변경
                    crop.setIsRegistered(true);
                    log.info("재배방법 분석 완료 및 등록 완료: Crop ID {}", cropId);
                } else {
                    crop.setAnalysisStatus(AnalysisStatus.FAILED);
                    log.error("재배방법 분석 실패: {}", analysisResult.getAnalysisMessage());
                }

                cropRepository.save(crop);

            } catch (Exception e) {
                log.error("재배방법 분석 중 오류 발생", e);
                // 실패 상태로 업데이트
                Crop crop = cropRepository.findById(cropId).orElse(null);
                if (crop != null) {
                    crop.setAnalysisStatus(AnalysisStatus.FAILED);
                    cropRepository.save(crop);
                }
            }
        });
    }

    /**
     * 새로운 통합 등록 방식: 텍스트 데이터와 이미지를 한 번에 처리하여 재배방법 분석 결과 반환
     */
    public Map<String, Object> analyzeCropWithData(User user, CropRegistrationDto cropData, MultipartFile imageFile) throws IOException {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 이미지 파일 저장
            String savedImagePath = saveImageFile(imageFile);

            // 2. 임시 작물 엔티티 생성 (DB 저장 없이)
            Crop tempCrop = new Crop();
            tempCrop.setUser(user);
            tempCrop.setName(cropData.getName());
            tempCrop.setStartAt(cropData.getStartAt());
            tempCrop.setEndAt(cropData.getEndAt());
            tempCrop.setCropImg(savedImagePath);
            tempCrop.setAnalysisStatus(AnalysisStatus.ANALYZING);
            tempCrop.setIsRegistered(false);
            tempCrop.setHarvest(false);

            // 3. 임시로 DB에 저장 (분석 완료 후 삭제 예정)
            Crop savedTempCrop = cropRepository.save(tempCrop);

            // 4. 동기적으로 재배방법 분석 수행 (REGISTRATION_ANALYSIS)
            CropAnalysisResult analysisResult = openAIService.analyzeCropImage(savedImagePath, com.planty.entity.crop.AnalysisType.REGISTRATION_ANALYSIS);

            // 5. 분석 결과 처리
            if (analysisResult.isSuccess()) {
                // 분석 성공 시 임시 작물 업데이트
                savedTempCrop.setAnalysisStatus(AnalysisStatus.COMPLETED);
                savedTempCrop.setEnvironment(analysisResult.getEnvironment());
                savedTempCrop.setTemperature(analysisResult.getTemperature());
                savedTempCrop.setHeight(analysisResult.getHeight());
                savedTempCrop.setHowTo(analysisResult.getHowTo());
                savedTempCrop.setIsRegistered(false); // 최종 등록 전까지는 false 유지

                cropRepository.save(savedTempCrop);

                // null이 아닌 필드들만 결과에 포함
                result.put("tempCropId", savedTempCrop.getId());
                result.put("analysisSuccess", true);
                result.put("analysisType", "REGISTRATION_ANALYSIS");
                result.put("message", "재배방법 분석이 완료되었습니다.");
                
                // null이 아닌 분석 결과만 포함
                if (analysisResult.getEnvironment() != null && !analysisResult.getEnvironment().trim().isEmpty()) {
                    result.put("environment", analysisResult.getEnvironment());
                }
                if (analysisResult.getTemperature() != null && !analysisResult.getTemperature().trim().isEmpty()) {
                    result.put("temperature", analysisResult.getTemperature());
                }
                if (analysisResult.getHeight() != null && !analysisResult.getHeight().trim().isEmpty()) {
                    result.put("height", analysisResult.getHeight());
                }
                if (analysisResult.getHowTo() != null && !analysisResult.getHowTo().trim().isEmpty()) {
                    result.put("howTo", analysisResult.getHowTo());
                }

            } else {
                // 분석 실패 시 임시 작물 삭제
                cropRepository.delete(savedTempCrop);
                
                result.put("tempCropId", null);
                result.put("analysisSuccess", false);
                result.put("analysisType", "REGISTRATION_ANALYSIS");
                result.put("message", "재배방법 분석에 실패했습니다: " + analysisResult.getAnalysisMessage());
            }

        } catch (Exception e) {
            log.error("재배방법 분석 중 오류 발생", e);
            result.put("tempCropId", null);
            result.put("analysisSuccess", false);
            result.put("analysisType", "REGISTRATION_ANALYSIS");
            result.put("message", "분석 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 이미지 파일 저장
     */
    public String saveImageFile(MultipartFile file) throws IOException {
        log.info("CropRegistrationAnalysisService.saveImageFile 시작 - 파일: {} (크기: {} bytes)", 
                file.getOriginalFilename(), file.getSize());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // 업로드 디렉토리 생성 (절대 경로 사용)
        String absoluteUploadPath = getAbsoluteUploadPath();
        File uploadDir = new File(absoluteUploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("업로드 디렉토리를 생성할 수 없습니다: " + absoluteUploadPath);
            }
        }

        // 파일 확장자 추출 (검증 없이)
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        // 고유한 파일명 생성 (확장자가 있는 경우에만 추가)
        String fileName = UUID.randomUUID().toString();
        if (extension != null && !extension.isEmpty()) {
            fileName += extension;
        }

        // 파일 저장
        File destinationFile = new File(uploadDir, fileName);
        try {
            file.transferTo(destinationFile);
            log.info("이미지 파일 저장 완료: {}", destinationFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("이미지 파일 저장 실패: {}", e.getMessage(), e);
            throw new IOException("이미지 파일 저장에 실패했습니다: " + e.getMessage());
        }

        return destinationFile.getAbsolutePath();
    }

    /**
     * 절대 업로드 경로 반환
     */
    private String getAbsoluteUploadPath() {
        // 로컬 개발용 경로
        String basePath = System.getProperty("user.dir") + "/uploads";
        
        // 카테고리별 하위 디렉토리 생성
        String categoryPath = basePath + "/crop";
        
        // 디렉토리가 없으면 생성
        File categoryDir = new File(categoryPath);
        if (!categoryDir.exists()) {
            boolean created = categoryDir.mkdirs();
            if (!created) {
                log.error("카테고리 디렉토리 생성 실패: {}", categoryPath);
                throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다: " + categoryPath);
            }
        }
        
        return categoryPath;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ""; // 확장자가 없는 경우 빈 문자열 반환
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }


}
