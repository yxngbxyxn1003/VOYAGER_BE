package com.planty.service.diary;

import com.planty.dto.diary.*;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.diary.DiaryRepository;
import com.planty.repository.user.UserRepository;
import com.planty.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;


// 재배일지 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    @SuppressWarnings("unused")
    private final StorageService storageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 재배일지 작성
    public void saveDiary(Integer userId, DiaryFormDto dto, List<String> imageUrls) {
        User user = userRepository.getReferenceById(userId);
        Crop crop = cropRepository.getReferenceById(dto.getCropId());

        // 이미지 개수 검증 (최대 9개)
        if (imageUrls != null && imageUrls.size() > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 9개까지만 업로드할 수 있습니다.");
        }

        // 재배일지 생성 및 데이터 삽입
        Diary diary = new Diary();
        diary.setUser(user);
        diary.setCrop(crop);
        diary.setTitle(dto.getTitle());
        diary.setContent(dto.getContent());
        
        // AI 진단 결과 포함 처리
        if (dto.getIncludeAnalysis() != null && dto.getIncludeAnalysis()) {
            if (dto.getAnalysis() != null && !dto.getAnalysis().trim().isEmpty()) {
                // 직접 입력한 분석 결과 사용
                diary.setAnalysis(dto.getAnalysis());
            } else if (dto.getDiagnosisData() != null && !dto.getDiagnosisData().trim().isEmpty()) {
                // 진단 결과 데이터 사용
                diary.setAnalysis(formatDiagnosisAnalysis(dto.getDiagnosisType(), dto.getDiagnosisData()));
            } else {
                // 작물의 기본 AI 분석 결과 사용
                String cropAnalysis = buildCropAnalysisText(crop);
                diary.setAnalysis(!cropAnalysis.isEmpty() ? cropAnalysis : null);
            }
        } else {
            diary.setAnalysis(null);
        }

        // 재배일지 저장 (이미지 저장 전)
        Diary savedDiary = diaryRepository.save(diary);

        // 재배일지 이미지 저장 및 썸네일 설정 (최대 9개, 첫 번째가 썸네일)
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<DiaryImage> imgs = createDiaryImages(savedDiary, imageUrls);
            savedDiary.setImages(imgs);
            diaryRepository.save(savedDiary); // 이미지와 함께 재저장
        }
    }
    
    // 작물의 AI 분석 결과를 텍스트로 변환
    private String buildCropAnalysisText(Crop crop) {
        StringBuilder analysisText = new StringBuilder();
        
        if (crop.getEnvironment() != null && !crop.getEnvironment().trim().isEmpty()) {
            analysisText.append("환경: ").append(crop.getEnvironment()).append("\n\n");
        }
        
        if (crop.getTemperature() != null && !crop.getTemperature().trim().isEmpty()) {
            analysisText.append("온도: ").append(crop.getTemperature()).append("\n\n");
        }
        
        if (crop.getHeight() != null && !crop.getHeight().trim().isEmpty()) {
            analysisText.append("높이: ").append(crop.getHeight()).append("\n\n");
        }
        
        if (crop.getHowTo() != null && !crop.getHowTo().trim().isEmpty()) {
            analysisText.append("재배법: ").append(crop.getHowTo()).append("\n\n");
        }
        
        return analysisText.toString().trim();
    }

    // 재배일지 이미지 생성 및 썸네일 설정
    private List<DiaryImage> createDiaryImages(Diary diary, List<String> imageUrls) {
        List<DiaryImage> imgs = new ArrayList<>();
        
        for (int i = 0; i < imageUrls.size() && i < 9; i++) { // 최대 9개까지 처리
            String imageUrl = imageUrls.get(i);
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                DiaryImage di = new DiaryImage();
                di.setDiary(diary);
                di.setDiaryImg(imageUrl);
                di.setThumbnail(i == 0); // 첫 번째 이미지만 썸네일로 설정
                imgs.add(di);
            }
        }
        
        return imgs;
    }

    // 재배일지 작성용 사용자 작물 목록 조회 (등록된 작물만)
    public List<Crop> getUserCrops(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        return cropRepository.findByUserAndIsRegisteredTrueOrderByCreatedAtDesc(user);
    }

    // 재배일지 상세 조회
    public DiaryDetailResDto getDiaryDetail(Integer id, Integer meId) {
        Diary diary = diaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재배일지를 찾을 수 없습니다."));

        // 재배일지 이미지 처리
        List<String> images = Optional.ofNullable(diary.getImages())
                .orElse(Collections.emptyList())
                .stream()
                .map(DiaryImage::getDiaryImg)
                .toList();

        // 썸네일 이미지 찾기
        String thumbnailImage = Optional.ofNullable(diary.getImages())
                .orElse(Collections.emptyList())
                .stream()
                .filter(DiaryImage::getThumbnail)
                .map(DiaryImage::getDiaryImg)
                .findFirst()
                .orElse(null);

        // 재배일지 정보
        DiaryDetailDto diaryDetailDto = DiaryDetailDto.builder()
                .diaryId(diary.getId())
                .cropId(diary.getCrop().getId())
                .cropName(diary.getCrop().getName())
                .title(diary.getTitle())
                .content(diary.getContent())
                .analysis(diary.getAnalysis())
                .images(images)
                .thumbnailImage(thumbnailImage)
                .createdAt(diary.getCreatedAt())
                .modifiedAt(diary.getModifiedAt())
                .build();

        // 소유자 여부
        boolean isOwner = diary.getUser().getId().equals(meId);

        // 프론트에 보내주는 Dto 반환
        return DiaryDetailResDto.builder()
                .diary(diaryDetailDto)
                .isOwner(isOwner)
                .build();
    }

    // 사용자별 재배일지 목록 조회
    public List<DiaryListDto> getUserDiaries(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        List<Diary> diaries = diaryRepository.findByUserOrderByCreatedAtDesc(user);

        return diaries.stream()
                .map(diary -> {
                    // 썸네일 이미지 찾기
                    String thumbnailImage = diary.getImages().stream()
                            .filter(DiaryImage::getThumbnail)
                            .map(DiaryImage::getDiaryImg)
                            .findFirst()
                            .orElse(null);

                    return DiaryListDto.builder()
                            .diaryId(diary.getId())
                            .title(diary.getTitle())
                            .cropName(diary.getCrop().getName())
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .build();
                })
                .toList();
    }

    // 작물별 재배일지 목록 조회
    public List<DiaryListDto> getCropDiaries(Integer cropId) {
        List<Diary> diaries = diaryRepository.findByCropIdOrderByCreatedAtDesc(cropId);

        return diaries.stream()
                .map(diary -> {
                    // 썸네일 이미지 찾기
                    String thumbnailImage = diary.getImages().stream()
                            .filter(DiaryImage::getThumbnail)
                            .map(DiaryImage::getDiaryImg)
                            .findFirst()
                            .orElse(null);

                    return DiaryListDto.builder()
                            .diaryId(diary.getId())
                            .title(diary.getTitle())
                            .cropName(diary.getCrop().getName())
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .build();
                })
                .toList();
    }

    // 재배일지 삭제
    public void deleteDiary(Integer diaryId, Integer userId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재배일지를 찾을 수 없습니다."));

        // 권한 확인 (본인의 재배일지만 삭제 가능)
        if (!diary.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        // 연관된 이미지 파일들을 파일 시스템에서 삭제
        if (diary.getImages() != null && !diary.getImages().isEmpty()) {
            for (DiaryImage image : diary.getImages()) {
                try {
                    // TODO 에러가 떠서 주석 처리 
                    //  storageService.delete(image.getDiaryImg());
                } catch (Exception e) {
                    // 파일 삭제 실패는 로그만 남기고 진행 (DB 삭제는 계속 진행)
                    System.err.println("이미지 파일 삭제 실패: " + image.getDiaryImg() + " - " + e.getMessage());
                }
            }
        }

        // 재배일지 삭제 (CASCADE로 관련 이미지들도 자동 삭제됨)
        diaryRepository.delete(diary);
    }



    // 썸네일 이미지 URL 추출 유틸리티 메서드
    @SuppressWarnings("unused")
    private String extractThumbnailImage(List<DiaryImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        
        return images.stream()
                .filter(DiaryImage::getThumbnail)
                .map(DiaryImage::getDiaryImg)
                .findFirst()
                .orElse(images.get(0).getDiaryImg()); // 썸네일이 없으면 첫 번째 이미지 반환
    }

    // 이미지 목록에서 썸네일 보장 (첫 번째 이미지를 썸네일로 설정)
    @SuppressWarnings("unused")
    private void ensureThumbnailExists(List<DiaryImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        // 기존 썸네일이 있는지 확인
        boolean hasThumbnail = images.stream().anyMatch(DiaryImage::getThumbnail);
        
        if (!hasThumbnail) {
            // 썸네일이 없으면 첫 번째 이미지를 썸네일로 설정
            images.get(0).setThumbnail(true);
        }
    }

    /**
     * 진단 결과 데이터를 재배일지용 텍스트로 포맷팅
     */
    private String formatDiagnosisAnalysis(AnalysisType diagnosisType, String diagnosisData) {
        try {
            JsonNode resultNode = objectMapper.readTree(diagnosisData);
            StringBuilder analysisText = new StringBuilder();

            switch (diagnosisType) {
                case CURRENT_STATUS:
                    analysisText.append("# 현재 상태 분석\n\n");
                    analysisText.append(resultNode.path("currentStatusSummary").asText("분석 결과 없음"));
                    break;
                    
                case DISEASE_CHECK:
                    analysisText.append("# 질병 진단 결과\n\n");
                    analysisText.append("**질병 상태:** ").append(resultNode.path("diseaseStatus").asText("")).append("\n\n");
                    analysisText.append("**상세 내용:** ").append(resultNode.path("diseaseDetails").asText("")).append("\n\n");
                    analysisText.append("**예방 및 치료 방법:** ").append(resultNode.path("preventionMethods").asText("")).append("\n");
                    break;
                    
                case QUALITY_MARKET:
                    analysisText.append("# 품질 및 시장성 분석\n\n");
                    analysisText.append("**상품 비율:** ").append(resultNode.path("marketRatio").asText("")).append("\n\n");
                    analysisText.append("**색상 품질:** ").append(resultNode.path("colorUniformity").asText("")).append("\n\n");
                    analysisText.append("**채도:** ").append(resultNode.path("saturation").asText("")).append("\n\n");
                    analysisText.append("**명도:** ").append(resultNode.path("brightness").asText("")).append("\n\n");
                    analysisText.append("**맛과 저장성:** ").append(resultNode.path("tasteStorage").asText("")).append("\n\n");
                    analysisText.append("**운송 저항성:** ").append(resultNode.path("transportResistance").asText("")).append("\n\n");
                    analysisText.append("**저장성 평가:** ").append(resultNode.path("storageEvaluation").asText("")).append("\n");
                    break;
            }

            return analysisText.toString();
            
        } catch (Exception e) {
            return "진단 결과 데이터를 처리하는 중 오류가 발생했습니다.";
        }
    }
}
