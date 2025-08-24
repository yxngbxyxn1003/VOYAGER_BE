package com.planty.service.mypage;


import com.planty.dto.mypage.MyHarvestCropResDto;
import com.planty.dto.mypage.MySellBoardResDto;
import com.planty.dto.mypage.ProfileResDto;
import com.planty.entity.board.Board;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.board.BoardRepository;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.mypage.MypageRepository;
import com.planty.repository.user.UserRepository;
import com.planty.storage.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import com.planty.storage.ImageUrlMapper;

// 마이페이지
@Service
@Transactional
@RequiredArgsConstructor
public class MypageService {

    private final MypageRepository mypageRepository;
    private final BoardRepository boardRepository;
    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ImageUrlMapper imageUrlMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 프로필 정보 불러오기
    public ProfileResDto getProfile(Integer userId) {
        // 유저 확인
        User user = mypageRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음: " + userId));

        return ProfileResDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getNickname())
                .point(user.getPoint())
                .profileImg(imageUrlMapper.toPublic(user.getProfileImg()))
                .build();
    }

    // 내가 쓴 판매 게시글 불러오기
    public List<MySellBoardResDto> getMySellBoard(Integer userId) {
        List<Board> boards = boardRepository.findMyBoardsOrderByStatusAndCreated(userId);

        return boards.stream()
                .map(MySellBoardResDto::of) // 1차 생성
                .map(dto ->
                        MySellBoardResDto.builder()
                                .boardId(dto.getBoardId())
                                .title(dto.getTitle())
                                .price(dto.getPrice())
                                .time(dto.getTime())
                                .sell(dto.getSell())
                                .thumbnailImg(imageUrlMapper.toPublic(dto.getThumbnailImg()))
                                .build()
                )
                .toList();
    }

    // 내 재배 완료된 작물 불러오기
    public List<MyHarvestCropResDto> getMyHarvestCrop(Integer userId) {
        List<Crop> crops = cropRepository.findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(userId);

        return crops.stream()
                .map(MyHarvestCropResDto::of) // 1차 생성
                .map(dto ->
                        MyHarvestCropResDto.builder()
                                .cropId(dto.getCropId())
                                .name(dto.getName())
                                .harvest(dto.getHarvest())
                                .thumbnail(imageUrlMapper.toPublic(dto.getThumbnail()))
                                .build()
                )
                .toList();
    }


    // 유저 프로필 수정
    @Transactional
    public void updateProfile(Integer meId, String nickname, MultipartFile profileImg) {
        // 유저 찾기
        User user = userRepository.findById(meId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        // 닉네임이 비어있지 않다면
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }

        // 이미지가 비어있지 않다면
        if (profileImg != null && !profileImg.isEmpty()) {
            // 이전 이미지
            String oldUrl = user.getProfileImg();

            // 저장: /uploads/profile/xxx 로 접근 가능하도록 folder="profile"
            String newUrl;
            try {
                newUrl = storageService.save(profileImg, "profile");
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 저장 실패", e);
            }

            user.setProfileImg(newUrl);
            userRepository.save(user); // URL 먼저 반영

            // 기존 파일 정리
            if (oldUrl != null && !oldUrl.isBlank()) {
                try { storageService.deleteByUrl(oldUrl); } catch (IOException ignore) {}
            }
        } else {
            // 이미지 없이 닉네임만 바뀌는 경우
            userRepository.save(user);
        }
    }

    // 비밀번호 수정
    public void updatePassword(Integer meId, String oldPassword, String newPassword) {
        // 유저 찾기
        User user = userRepository.findById(meId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        // 비밀번호 필드 검사
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION");
        }

        // 기존 비밀번호 확인
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD");
        }

        // 새 비밀번호 암호화 후 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
