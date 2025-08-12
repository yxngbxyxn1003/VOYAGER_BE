// src/main/java/com/planty/config/jwt/JwtAuthenticationFilter.java
package com.planty.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final Set<String> whitelist = Set.of(
            "/api/users/login",
            "/api/users/signup"
    );

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    // 화이트리스트는 필터 건너뜀
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // OPTIONS (CORS preflight)도 스킵
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || whitelist.contains(path);
    }

    // 인증 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);

            try {
                // 1) 토큰에서 userId(subject) 추출 (서명/만료 검증 포함)
                String userId = jwtProvider.getSubject(token);

                // 2) DB에서 유저 로드
                UserDetails user = userDetailsService.loadUserByUsername(userId);

                // 3) 인증 객체 생성해서 컨텍스트에 저장
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception ex) {
                // 토큰이 유효하지 않으면 인증 컨텍스트를 비운 채로 통과 → 뒤에서 401 처리됨
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
