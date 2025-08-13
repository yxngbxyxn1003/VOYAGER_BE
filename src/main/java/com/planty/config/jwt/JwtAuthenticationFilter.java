package com.planty.config.jwt;

import com.planty.config.CustomUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;


@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final Set<String> whitelist = Set.of(
            "/api/users/login",
            "/api/users/signup"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    // 화이트리스트 + OPTIONS 스킵
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        // 필요하면 패턴 매칭 허용: /api/users/** 같은
        for (String p : whitelist) {
            if (pathMatcher.match(p, path)) return true;
        }
        return false;
    }

    // 인증 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 토큰 꺼내 오기
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7).trim();

            // 실수로 'Bearer '가 한 번 더 들어온 경우 방어
            if (token.startsWith("Bearer ")) token = token.substring(7).trim();

            // 따옴표로 감싸져 온 경우 제거
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
                token = token.substring(1, token.length() - 1);
            }

            // 토큰 내부의 모든 공백/개행 제거 (정상 JWT엔 공백이 없어야 함)
            token = token.replaceAll("\\s+", "");

            try {
                // 1) 토큰 파싱
                String userId = jwtProvider.getSubject(token);

                // 2) 유저 로드
                CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(userId);

                // 3) 컨텍스트 설정
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        } else {
            // 토큰 아예 없음
            log.debug("[JWT] no bearer header for {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
