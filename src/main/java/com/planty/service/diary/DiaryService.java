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
