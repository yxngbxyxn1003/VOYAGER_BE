package com.planty.service.diary;

import com.planty.dto.diary.*;
import com.planty.dto.crop.HomeCropDto;
import com.planty.entity.crop.AnalysisType;
import com.planty.entity.crop.Crop;
import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.diary.DiaryRepository;
import com.planty.repository.user.UserRepository;
import com.planty.service.crop.CropService;
import com.planty.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

// 재배일지 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final CropService cropService;
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
        
        // AI 분석 결과 포함 처리 (재배일지 전용)
        if (dto.getIncludeAnalysis() != null && dto.getIncludeAnalysis()) {
            if (dto.getAnalysis() != null && !dto.getAnalysis().trim().isEmpty()) {
                // 사용자가 직접 입력한 분석 결과 사용
                diary.setAnalysis(dto.getAnalysis());
            } else if (dto.getDiagnosisData() != null && !dto.getDiagnosisData().trim().isEmpty()) {
                // 진단 결과 데이터를 원본 그대로 사용 (포맷팅 없음)
                diary.setAnalysis(dto.getDiagnosisData());
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

    /**
     * 진단결과 기반 재배일지 생성 (cropId 없이)
     */
    public Diary createDiagnosisBasedDiary(Integer userId, String title, String content, 
                                         String diagnosisType, Map<String, Object> diagnosisResult, 
                                         Boolean includeDiagnosis, List<String> imageUrls) {
        User user = userRepository.getReferenceById(userId);

        // 이미지 개수 검증 (최대 9개)
        if (imageUrls != null && imageUrls.size() > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 9개까지만 업로드할 수 있습니다.");
        }

        // 재배일지 생성
        Diary diary = new Diary();
        diary.setUser(user);
        diary.setCrop(null); // 진단결과 기반이므로 cropId 없음
        diary.setTitle(title);
        diary.setContent(content);
        
        // 진단 결과 포함 처리
        if (includeDiagnosis != null && includeDiagnosis && diagnosisResult != null) {
            diary.setIncludeDiagnosis(true);
            diary.setDiagnosisType(AnalysisType.valueOf(diagnosisType));
            
            // 진단 결과를 JSON으로 저장
            try {
                String diagnosisData = objectMapper.writeValueAsString(diagnosisResult);
                diary.setDiagnosisData(diagnosisData);
                
                // 진단 결과를 분석 필드에도 포함
                StringBuilder analysisContent = new StringBuilder();
                analysisContent.append("## AI 진단 결과\n");
                analysisContent.append("**분석 유형:** ").append(getDiagnosisTypeName(diagnosisType)).append("\n\n");
                
                // 진단 결과 내용 추가
                switch (diagnosisType) {
                    case "CURRENT_STATUS":
                        analysisContent.append("**현재 상태:** ").append(diagnosisResult.getOrDefault("currentStatusSummary", "")).append("\n\n");
                        break;
                    case "DISEASE_CHECK":
                        analysisContent.append("**질병 상태:** ").append(diagnosisResult.getOrDefault("diseaseStatus", "")).append("\n");
                        analysisContent.append("**상세 내용:** ").append(diagnosisResult.getOrDefault("diseaseDetails", "")).append("\n");
                        analysisContent.append("**예방 및 치료 방법:** ").append(diagnosisResult.getOrDefault("preventionMethods", "")).append("\n\n");
                        break;
                    case "QUALITY_MARKET":
                        analysisContent.append("**상품 비율:** ").append(diagnosisResult.getOrDefault("marketRatio", "")).append("\n");
                        analysisContent.append("**색상 품질:** ").append(diagnosisResult.getOrDefault("colorUniformity", "")).append("\n");
                        analysisContent.append("**저장성:** ").append(diagnosisResult.getOrDefault("storageEvaluation", "")).append("\n\n");
                        break;
                }
                
                diary.setAnalysis(analysisContent.toString());
                
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "진단 결과 처리 중 오류가 발생했습니다.");
            }
        } else {
            diary.setIncludeDiagnosis(false);
            diary.setDiagnosisType(null);
            diary.setDiagnosisData(null);
            diary.setAnalysis(null);
        }

        // 재배일지 저장
        Diary savedDiary = diaryRepository.save(diary);

        // 이미지 저장 및 썸네일 설정
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<DiaryImage> imgs = createDiaryImages(savedDiary, imageUrls);
            savedDiary.setImages(imgs);
            diaryRepository.save(savedDiary);
        }

        return savedDiary;
    }

    /**
     * 진단 타입 이름 반환
     */
    private String getDiagnosisTypeName(String diagnosisType) {
        return switch (diagnosisType) {
            case "CURRENT_STATUS" -> "현재 상태 분석";
            case "DISEASE_CHECK" -> "질병 여부 분석";
            case "QUALITY_MARKET" -> "품질 및 시장성 분석";
            default -> "진단 분석";
        };
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
    public List<HomeCropDto> getUserCrops(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        List<Crop> crops = cropRepository.findByUserAndIsRegisteredTrueOrderByCreatedAtDesc(user);
        
        return crops.stream()
                .map(crop -> {
                    // 카테고리명 추출 (안전하게 처리)
                    String categoryName = "기타";
                    if (crop.getCategories() != null && !crop.getCategories().isEmpty()) {
                        categoryName = crop.getCategories().get(0).getCategoryName();
                    }
                    
                    return HomeCropDto.of(crop);
                })
                .toList();
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

        // 재배일지 정보 (crop이 null일 수 있음)
        DiaryDetailDto diaryDetailDto = DiaryDetailDto.builder()
                .diaryId(diary.getId())
                .cropId(diary.getCrop() != null ? diary.getCrop().getId() : null)
                .cropName(diary.getCrop() != null ? diary.getCrop().getName() : "진단결과 기반")
                .title(diary.getTitle())
                .content(diary.getContent())
                .analysis(diary.getAnalysis())
                .images(images)
                .thumbnailImage(thumbnailImage)
                .createdAt(diary.getCreatedAt())
                .modifiedAt(diary.getModifiedAt())
                .diaryType(getDiaryType(diary))
                .diagnosisType(diary.getDiagnosisType() != null ? diary.getDiagnosisType().toString() : null)
                .includeDiagnosis(diary.getIncludeDiagnosis())
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
                            .cropName(diary.getCrop() != null ? diary.getCrop().getName() : "진단결과 기반")
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .diaryType(getDiaryType(diary))
                            .diagnosisType(diary.getDiagnosisType() != null ? diary.getDiagnosisType().toString() : null)
                            .build();
                })
                .toList();
    }

    // 내 재배일지 목록 조회 (사용자별 모든 재배일지)
    public List<DiaryListDto> getMyDiariesByCategory(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        
        // 사용자의 모든 재배일지 조회 (카테고리 필터링 없음)
        List<Diary> diaries = diaryRepository.findByUserOrderByCreatedAtDesc(user);
        
        // 간단한 로깅 (문제 파악용)
        System.out.println("사용자 ID: " + userId + ", 조회된 재배일지 수: " + diaries.size());
        
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
                            .cropName(diary.getCrop() != null ? diary.getCrop().getName() : "진단결과 기반")
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .diaryType(getDiaryType(diary))
                            .diagnosisType(diary.getDiagnosisType() != null ? diary.getDiagnosisType().toString() : null)
                            .build();
                })
                .toList();
    }

    /**
     * 사용자별 일반 재배일지 목록 조회 (crop이 있는 경우만)
     */
    public List<DiaryListDto> getUserCropBasedDiaries(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        List<Diary> diaries = diaryRepository.findByUserAndCropIsNotNullOrderByCreatedAtDesc(user);
        
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
                            .diaryType("CROP_BASED")
                            .diagnosisType(null)
                            .build();
                })
                .toList();
    }

    /**
     * 사용자별 진단결과 기반 재배일지 목록 조회 (crop이 없는 경우만)
     */
    public List<DiaryListDto> getUserDiagnosisBasedDiaries(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        List<Diary> diaries = diaryRepository.findByUserAndCropIsNullOrderByCreatedAtDesc(user);
        
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
                            .cropName("진단결과 기반")
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .diaryType("DIAGNOSIS_BASED")
                            .diagnosisType(diary.getDiagnosisType() != null ? diary.getDiagnosisType().toString() : null)
                            .build();
                })
                .toList();
    }

    /**
     * 사용자별 모든 재배일지 목록 조회 (일반 + 진단결과 기반)
     */
    public List<DiaryListDto> getAllUserDiaries(Integer userId) {
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

                    // 재배일지 타입 구분
                    String cropName;
                    if (diary.getCrop() != null) {
                        // 일반 재배일지 (작물 기반)
                        cropName = diary.getCrop().getName();
                    } else if (diary.getIncludeDiagnosis() != null && diary.getIncludeDiagnosis() && 
                               diary.getDiagnosisType() != null) {
                        // 진단결과 기반 재배일지
                        cropName = "진단결과 기반 (" + getDiagnosisTypeDisplayName(diary.getDiagnosisType()) + ")";
                    } else {
                        // 일반 재배일지 (작물 없이 시작)
                        cropName = "일반 재배일지";
                    }

                    return DiaryListDto.builder()
                            .diaryId(diary.getId())
                            .title(diary.getTitle())
                            .cropName(cropName)
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .diaryType(getDiaryType(diary))
                            .diagnosisType(diary.getDiagnosisType() != null ? diary.getDiagnosisType().toString() : null)
                            .build();
                })
                .toList();
    }

    /**
     * 진단 타입을 표시용 이름으로 변환
     */
    private String getDiagnosisTypeDisplayName(AnalysisType diagnosisType) {
        return switch (diagnosisType) {
            case CURRENT_STATUS -> "현재상태분석";
            case DISEASE_CHECK -> "질병여부분석";
            case QUALITY_MARKET -> "품질시장성분석";
            default -> "진단분석";
        };
    }

    /**
     * 재배일지 타입을 판별하여 반환
     */
    private String getDiaryType(Diary diary) {
        if (diary.getCrop() != null) {
            return "CROP_BASED"; // 작물 기반 재배일지
        } else if (diary.getIncludeDiagnosis() != null && diary.getIncludeDiagnosis() && 
                   diary.getDiagnosisType() != null) {
            return "DIAGNOSIS_BASED"; // 진단결과 기반 재배일지
        } else {
            return "GENERAL"; // 일반 재배일지 (작물 없이 시작)
        }
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
                            .cropName(diary.getCrop() != null ? diary.getCrop().getName() : "진단결과 기반")
                            .thumbnailImage(thumbnailImage)
                            .createdAt(diary.getCreatedAt())
                            .diaryType(getDiaryType(diary))
                            .diagnosisType(diary.getDiagnosisType() != null ? diary.getDiagnosisType().toString() : null)
                            .build();
                })
                .toList();
    }

    // 재배일지 수정
    public void updateDiary(Integer diaryId, Integer userId, DiaryUpdateDto dto, List<String> newImageUrls) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재배일지를 찾을 수 없습니다."));

        // 권한 확인 (본인의 재배일지만 수정 가능)
        if (!diary.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }

        // 기본 정보 수정 (이미지와 무관하게)
        diary.setTitle(dto.getTitle());
        diary.setContent(dto.getContent());

        // AI 분석 결과 수정
        if (dto.getIncludeAnalysis() != null && dto.getIncludeAnalysis()) {
            if (dto.getAnalysis() != null && !dto.getAnalysis().trim().isEmpty()) {
                // 직접 입력한 분석 결과 사용
                diary.setAnalysis(dto.getAnalysis());
            } else if (dto.getDiagnosisData() != null && !dto.getDiagnosisData().trim().isEmpty()) {
                // 진단 결과 데이터를 원본 그대로 사용
                diary.setAnalysis(dto.getDiagnosisData());
            }
        } else {
            diary.setAnalysis(null);  // AI 분석 결과 미포함
        }

        // 진단 결과 포함 여부 수정
        if (dto.getIncludeDiagnosis() != null) {
            diary.setIncludeDiagnosis(dto.getIncludeDiagnosis());
            
            if (dto.getIncludeDiagnosis()) {
                // 진단 결과 포함하는 경우
                if (dto.getDiagnosisType() != null && dto.getDiagnosisData() != null) {
                    try {
                        diary.setDiagnosisType(AnalysisType.valueOf(dto.getDiagnosisType()));
                        diary.setDiagnosisData(dto.getDiagnosisData());
                        
                        // 진단 결과를 분석 필드에도 포함
                        Map<String, Object> diagnosisResult = objectMapper.readValue(dto.getDiagnosisData(), Map.class);
                        StringBuilder analysisContent = new StringBuilder();
                        analysisContent.append("## AI 진단 결과\n");
                        analysisContent.append("**분석 유형:** ").append(getDiagnosisTypeName(dto.getDiagnosisType())).append("\n\n");
                        
                        // 진단 결과 내용 추가
                        switch (dto.getDiagnosisType()) {
                            case "CURRENT_STATUS":
                                analysisContent.append("**현재 상태:** ").append(diagnosisResult.getOrDefault("currentStatusSummary", "")).append("\n\n");
                                break;
                            case "DISEASE_CHECK":
                                analysisContent.append("**질병 상태:** ").append(diagnosisResult.getOrDefault("diseaseStatus", "")).append("\n");
                                analysisContent.append("**상세 내용:** ").append(diagnosisResult.getOrDefault("diseaseDetails", "")).append("\n");
                                analysisContent.append("**예방 및 치료 방법:** ").append(diagnosisResult.getOrDefault("preventionMethods", "")).append("\n\n");
                                break;
                            case "QUALITY_MARKET":
                                analysisContent.append("**상품 비율:** ").append(diagnosisResult.getOrDefault("marketRatio", "")).append("\n");
                                analysisContent.append("**색상 품질:** ").append(diagnosisResult.getOrDefault("colorUniformity", "")).append("\n");
                                analysisContent.append("**저장성:** ").append(diagnosisResult.getOrDefault("storageEvaluation", "")).append("\n\n");
                                break;
                        }
                        
                        diary.setAnalysis(analysisContent.toString());
                        
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진단 결과 데이터 처리 중 오류가 발생했습니다.");
                    }
                }
            } else {
                // 진단 결과 미포함하는 경우
                diary.setDiagnosisType(null);
                diary.setDiagnosisData(null);
                // analysis 필드는 기존 내용 유지 (사용자가 직접 입력한 내용이 있을 수 있음)
            }
        }

        // 이미지가 있는 경우에만 이미지 수정 처리
        if (diary.getImages() != null || (newImageUrls != null && !newImageUrls.isEmpty())) {
            updateDiaryImages(diary, dto, newImageUrls);
        }

        // 재배일지 저장
        diaryRepository.save(diary);
    }

    // 재배일지 이미지 수정 처리
    private void updateDiaryImages(Diary diary, DiaryUpdateDto dto, List<String> newImageUrls) {
        // null 안전 처리
        List<DiaryImage> currentImages = diary.getImages() != null ? diary.getImages() : new ArrayList<>();
        
        // 삭제할 이미지 처리
        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {
            currentImages.removeIf(image -> {
                if (dto.getImagesToDelete().contains(image.getDiaryImg())) {
                    // 파일 시스템에서 이미지 삭제 시도
                    try {
                        // TODO: StorageService 삭제 기능 구현 후 활성화
                        // storageService.delete(image.getDiaryImg());
                    } catch (Exception e) {
                        System.err.println("이미지 파일 삭제 실패: " + image.getDiaryImg() + " - " + e.getMessage());
                    }
                    return true;
                }
                return false;
            });
        }

        // 새 이미지 추가
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            for (String imageUrl : newImageUrls) {
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    DiaryImage newImage = new DiaryImage();
                    newImage.setDiary(diary);
                    newImage.setDiaryImg(imageUrl);
                    newImage.setThumbnail(false); // 새 이미지는 일단 썸네일이 아님
                    currentImages.add(newImage);
                }
            }
        }

        // 전체 이미지 개수 검증 (최대 9개)
        if (currentImages.size() > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 9개까지만 업로드할 수 있습니다.");
        }

        // 썸네일 설정 보장
        if (!currentImages.isEmpty()) {
            boolean hasThumbnail = currentImages.stream().anyMatch(DiaryImage::getThumbnail);
            if (!hasThumbnail) {
                currentImages.get(0).setThumbnail(true); // 첫 번째 이미지를 썸네일로 설정
            }
        }

        diary.setImages(currentImages);
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
}
