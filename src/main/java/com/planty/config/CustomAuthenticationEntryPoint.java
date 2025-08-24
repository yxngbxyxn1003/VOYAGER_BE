package com.planty.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planty.common.ApiError;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


// 인증되지 않은 사용자 처리
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader("Authorization");
        
        log.error("[AUTH] Authentication failed for {} {} - Auth header: {}", method, path, authHeader);
        log.error("[AUTH] Authentication exception: {}", authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // (선택) Bearer 토큰 표준 헤더 — 필요 없으면 삭제 가능
        // response.setHeader("WWW-Authenticate", "Bearer realm=\"api\", error=\"invalid_token\"");

        // 에러 JSON에 넣을 데이터
        var entity = ApiError.of(401, "UNAUTHORIZED", "인증되지 않은 사용자입니다.");
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 에러 JSON 생성
        om.writeValue(response.getWriter(), entity.getBody());
    }
}
