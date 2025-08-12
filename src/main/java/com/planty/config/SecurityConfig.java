package com.planty.config;

import com.planty.service.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


// 보안
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    // (선택) JWT 쓰면 주입해서 아래 addFilterBefore에 등록
    // private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            UserService userService,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomAuthenticationEntryPoint authenticationEntryPoint
            // , JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.userService = userService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        // this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // 비밀번호 해시 저장
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 인증 처리기를 인증 로직에서 호출
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API: CSRF 비활성화
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // 기본값. 필요 시 CorsConfigurationSource 빈 정의해서 커스터마이즈

                // 세션 미사용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 폼 로그인/로그아웃 제거
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                // 유저 정보 소스
                .userDetailsService(userService)

                // 권한 매핑
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 없이 접근 가능한 공개 API
                        .requestMatchers("/api/users/signup").permitAll()
                        .requestMatchers("/api/users/login").permitAll()

                        // 어드민 보호
                        .requestMatchers("/api/admin/**").denyAll()

                        // 나머진 인증 필요
                        .anyRequest().authenticated()
                )

                // 예외 처리(JSON 응답)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        // JWT 사용 시: 사용자 정의 필터 추가
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // JWT 준비 전, 임시로 Basic 인증으로 개발 테스트 가능 (원하면 주석 해제)
        // http.httpBasic(Customizer.withDefaults());

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용 주소 설정
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));

        // 허용 메서드 설정
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 헤더 목록 설정
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // 브라우저 자격 증명 허용
        config.setAllowCredentials(true);

        // 권한 헤더를 읽을 수 있게 설정
        config.setExposedHeaders(List.of("Authorization"));

        // URL 패턴에 따라 서로 다른 CORS 정책 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
