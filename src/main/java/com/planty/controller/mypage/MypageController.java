package com.planty.controller.mypage;

import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;
import com.planty.dto.mypage.PasswordFormDto;
import com.planty.dto.mypage.ProfilePatchFormDto;
import com.planty.service.mypage.MypageService;
import com.planty.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


// 마이페이지
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;
    private final UserService userService;

    // 프로필 정보 가져오기
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 프로필 정보 반환
        return ResponseEntity.ok(mypageService.getProfile(me.getId()));
    }

    // 내가 쓴 판매 게시글 가져오기
    @GetMapping("/boards")
    public ResponseEntity<?> getBoards(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 내가 쓴 판매 게시글 반환
        return ResponseEntity.ok(mypageService.getMySellBoard(me.getId()));
    }

    // 재배 완료된 작물 가져오기
    @GetMapping("/harvest-crop")
    public ResponseEntity<?> getHarvestCrop(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 재배 완료된 작물 가져오기
        return ResponseEntity.ok(mypageService.getMyHarvestCrop(me.getId()));
    }

    // 회원 정보 수정
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal CustomUserDetails me,
            @ModelAttribute ProfilePatchFormDto dto
    ) {
        // 유저 확인
        if (me == null) return ResponseEntity.status(401).build();

        // 프로필 정보 수정
        mypageService.updateProfile(me.getId(), dto.getNickname(), dto.getProfileImg());

        // 성공 응답 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }

    // 비밀번호 수정
    @PostMapping(value = "/password")
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestBody PasswordFormDto dto
    ) {
        // 유저 확인
        if (me == null) return ResponseEntity.status(401).build();

        // 비밀번호 수정
        mypageService.updatePassword(me.getId(), dto.getOldPassword(), dto.getNewPassword());

        // 성공 응답 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }
}
