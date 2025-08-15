package com.planty.service.diary;

import com.planty.dto.diary.*;
import com.planty.entity.crop.Crop;
import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.diary.DiaryRepository;
import com.planty.repository.user.UserRepository;
import com.planty.storage.StorageService;
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
    private final StorageService storageService;

    // 재배일지 작성
    public void saveDiary(Integer userId, DiaryFormDto dto, List<String> imageUrls) {
        User user = userRepository.getReferenceById(userId);
        Crop crop = cropRepository.getReferenceById(dto.getCropId());

        // 재배일지 생성 및 데이터 삽입
        Diary diary = new Diary();
        diary.setUser(user);
        diary.setCrop(crop);
        diary.setTitle(dto.getTitle());
        diary.setContent(dto.getContent());
        diary.setAnalysis(dto.getAnalysis()); // AI 분석 결과

        // 재배일지 이미지 삽입
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<DiaryImage> imgs = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                DiaryImage di = new DiaryImage();
                di.setDiary(diary);
                di.setDiaryImg(imageUrls.get(i));
                di.setThumbnail(i == 0); // 첫 번째 이미지를 썸네일로 설정
                imgs.add(di);
            }
            diary.setImages(imgs);
        }

        // 재배일지 저장
        diaryRepository.save(diary);
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

        // 재배일지 정보
        DiaryDetailDto diaryDetailDto = DiaryDetailDto.builder()
                .diaryId(diary.getId())
                .cropId(diary.getCrop().getId())
                .cropName(diary.getCrop().getName())
                .title(diary.getTitle())
                .content(diary.getContent())
                .analysis(diary.getAnalysis())
                .images(images)
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

    // 사용자의 작물 목록 조회 (재배일지 작성용)
    public List<Crop> getUserCrops(Integer userId) {
        User user = userRepository.getReferenceById(userId);
        return user.getCrops();
    }
}
