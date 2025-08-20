package com.planty.service.crop;

import com.planty.dto.crop.CropAnalysisResult;
import com.planty.dto.crop.CropDetailAnalysisResult;
import com.planty.dto.crop.CropRegistrationDto;

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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final OpenAIService openAIService;

    @Value("${app.upload.path:uploads/crop/}")
    private String uploadPath;
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

        // 2. 이미지 파일 저장
        String savedImagePath = saveImageFile(imageFile);
        crop.setCropImg(savedImagePath);
        crop.setAnalysisStatus(AnalysisStatus.ANALYZING);

        Crop savedCrop = cropRepository.save(crop);

        // 3. 비동기로 이미지 분석 시작
        analyzeImageAsync(savedCrop.getId(), savedImagePath);

        return savedCrop;
    }

    /**
     * 작물 이미지 업로드 및 분석 시작 (기존 방식 - 호환성 유지)
     */
    public Crop uploadCropImage(User user, MultipartFile imageFile) throws IOException {
        // 1. 이미지 파일 저장
        String savedImagePath = saveImageFile(imageFile);

        // 2. 작물 엔티티 생성 (분석 대기 상태)
        Crop crop = new Crop();
        crop.setUser(user);
        crop.setCropImg(savedImagePath);
        crop.setAnalysisStatus(AnalysisStatus.PENDING);
        crop.setIsRegistered(false);
        crop.setHarvest(false);

        Crop savedCrop = cropRepository.save(crop);

        // 3. 비동기로 이미지 분석 시작
        analyzeImageAsync(savedCrop.getId(), savedImagePath);

        return savedCrop;
    }
//
    /**
     * 비동기 이미지 분석
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

                // OpenAI로 이미지 분석
                CropAnalysisResult analysisResult = openAIService.analyzeCropImage(imagePath);

                // 분석 결과로 작물 정보 업데이트
                if (analysisResult.isSuccess()) {
                    crop.setAnalysisStatus(AnalysisStatus.COMPLETED);
                    crop.setEnvironment(analysisResult.getEnvironment());
                    crop.setTemperature(analysisResult.getTemperature());
                    crop.setHeight(analysisResult.getHeight());
                    crop.setHowTo(analysisResult.getHowTo());
                    // AI 분석 완료 시 자동으로 등록 완료 상태로 변경
                    crop.setIsRegistered(true);
                    log.info("작물 분석 완료 및 등록 완료: Crop ID {}", cropId);
                } else {
                    crop.setAnalysisStatus(AnalysisStatus.FAILED);
                    log.error("이미지 분석 실패: {}", analysisResult.getAnalysisMessage());
                }

                cropRepository.save(crop);

            } catch (Exception e) {
                log.error("이미지 분석 중 오류 발생", e);
                // 실패 상태로 업데이트
                Crop crop = cropRepository.findById(cropId).orElse(null);
                if (crop != null) {
                    crop.setAnalysisStatus(AnalysisStatus.FAILED);
                    cropRepository.save(crop);
                }
            }
        });
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
    /**
     * 이미지 파일 저장
     */
    private String saveImageFile(MultipartFile file) throws IOException {
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

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        // 허용된 이미지 확장자 검증
        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG, GIF만 허용됩니다.");
        }

        // 고유한 파일명 생성
        String fileName = UUID.randomUUID().toString() + extension;

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
        
        // 배포용 경로 (주석처리)
        /*
        // EC2 배포를 위해 절대 경로 사용
        String basePath = "/home/ec2-user/planty/uploads";
        
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
        */
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // 기본 확장자
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 유효한 이미지 확장자 검증
     */
    private boolean isValidImageExtension(String extension) {
        return extension.matches("\\.(jpg|jpeg|png|gif)$");
    }
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
     * 작물 세부 분석 (현재상태, 질병여부, 품질/시장성)
     */
    public CropDetailAnalysisResult analyzeCropDetail(Crop crop, AnalysisType analysisType) {
        try {
            // 작물 이미지가 없는 경우
            if (crop.getCropImg() == null || crop.getCropImg().trim().isEmpty()) {
                return new CropDetailAnalysisResult(false, "분석할 이미지가 없습니다.", analysisType);
            }

            // 이미지 파일 존재 확인
            File imageFile = new File(crop.getCropImg());
            if (!imageFile.exists()) {
                return new CropDetailAnalysisResult(false, "이미지 파일을 찾을 수 없습니다.", analysisType);
            }

            // OpenAI를 통한 세부 분석
            CropDetailAnalysisResult result = openAIService.analyzeCropDetail(crop.getCropImg(), analysisType);

            log.info("작물 세부 분석 완료 - 작물 ID: {}, 분석 타입: {}, 성공: {}",
                    crop.getId(), analysisType, result.isSuccess());

            return result;

        } catch (Exception e) {
            log.error("작물 세부 분석 중 오류 발생 - 작물 ID: {}, 분석 타입: {}", crop.getId(), analysisType, e);
            return new CropDetailAnalysisResult(false, "분석 중 오류가 발생했습니다: " + e.getMessage(), analysisType);
        }
    }

    /**
     * 새로운 통합 등록 방식: 텍스트 데이터와 이미지를 한 번에 처리하여 분석 결과 반환
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

            // 4. 동기적으로 이미지 분석 수행 (비동기 대신 즉시 결과 반환)
            CropAnalysisResult analysisResult = openAIService.analyzeCropImage(savedImagePath);

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

                // 결과 반환
                result.put("tempCropId", savedTempCrop.getId());
                result.put("analysisSuccess", true);
                result.put("environment", analysisResult.getEnvironment());
                result.put("temperature", analysisResult.getTemperature());
                result.put("height", analysisResult.getHeight());
                result.put("howTo", analysisResult.getHowTo());
                result.put("message", "이미지 분석이 완료되었습니다.");

            } else {
                // 분석 실패 시 임시 작물 삭제
                cropRepository.delete(savedTempCrop);
                
                result.put("tempCropId", null);
                result.put("analysisSuccess", false);
                result.put("message", "이미지 분석에 실패했습니다: " + analysisResult.getAnalysisMessage());
            }

        } catch (Exception e) {
            log.error("작물 데이터 분석 중 오류 발생", e);
            result.put("tempCropId", null);
            result.put("analysisSuccess", false);
            result.put("message", "분석 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
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
}
