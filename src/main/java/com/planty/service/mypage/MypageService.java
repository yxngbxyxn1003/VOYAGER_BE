package com.planty.service.mypage;


import com.planty.dto.mypage.ProfileResDto;
import com.planty.entity.user.User;
import com.planty.repository.mypage.MypageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// 마이페이지
@Service
@Transactional
@RequiredArgsConstructor
public class MypageService {

    private final MypageRepository mypageRepository;

    // 프로필 정보 불러오기
    public ProfileResDto getProfile(Integer userId) {
        User user = mypageRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음: " + userId));

        return ProfileResDto.builder()
                .id(user.getId())
                .name(user.getNickname())
                .point(user.getPoint())
                .profileImg(user.getProfileImg())
                .build();
    }
}
