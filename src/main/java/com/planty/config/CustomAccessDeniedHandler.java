package com.planty.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planty.common.ApiError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


// 권한이 없는 사용자
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        // 에러 JSON에 넣을 데이터
        var entity = ApiError.of(403, "FORBIDDEN", "접근 권한이 없습니다.");
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");

        // 에러 JSON 삽입
        om.writeValue(response.getWriter(), entity.getBody());
    }
}
