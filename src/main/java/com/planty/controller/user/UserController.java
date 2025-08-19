package com.planty.controller.user;

import com.planty.common.ApiSuccess;
import com.planty.config.jwt.JwtProvider;
import com.planty.dto.user.LoginFormDto;
import com.planty.dto.user.SignupFormDto;
import com.planty.entity.user.User;
import com.planty.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupFormDto signupFormDto) {
        // 비밀번호 해시 + 저장
        User user = User.createUser(signupFormDto, passwordEncoder);
        userService.saveUser(user);

        // 컨벤션: 데이터 없을 때 status + message
        return ResponseEntity.status(201).body(new ApiSuccess(201, "성공적으로 처리되었습니다."));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginFormDto loginFormDto) {

        UserDetails user;

        // id로 유저 조회
        try {
            user = userService.loadUserByUsername(loginFormDto.getUserId());
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // 입력된 비번과 저장된 해시 비교
        if (!passwordEncoder.matches(loginFormDto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // JWT 발급
        String accessToken  = jwtProvider.createAccessToken(user.getUsername());

        // JSON 응답 데이터
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 200);
        body.put("message", "로그인 성공");
        body.put("accessToken", accessToken);

        // JSON 응답 생성
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(body);

    }
}
