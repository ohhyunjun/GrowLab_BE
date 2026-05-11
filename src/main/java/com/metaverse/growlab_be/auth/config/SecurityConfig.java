package com.metaverse.growlab_be.auth.config;

import com.metaverse.growlab_be.auth.filter.JwtAuthenticationFilter;
import com.metaverse.growlab_be.auth.handler.CustomAccessDeniedHandler;
import com.metaverse.growlab_be.auth.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // 스프링부트 설정 빈으로 등록
@EnableWebSecurity // 스프링시큐리티 활성화
@EnableMethodSecurity // 스프링시큐리티의 RBAC을 위한 메서드 권한 제어 활성화
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // Authentication Manager 빈 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 스프링시큐리티의 필터체인을 빈으로 등록, 규칙 정의
    // 참고 공식 문서 https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html#servlet-security-filters
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 커스텀 CORS 설정을 스프링 시큐리티 필터 체인에 통합
                .cors(Customizer.withDefaults())
                // CSRF(Cross-Site Request Forgery) 교차 요청 위조 공격
                // JWT 토큰 기반 인증 방식은 CSRF를 방어하는 수단이므로 불필요함
                .csrf(AbstractHttpConfigurer::disable)
                // 토큰 기반 인증방식에서 불필요함
                .httpBasic(AbstractHttpConfigurer::disable)
                // 스프링 시큐리티가 제공하는 기본 로그인 폼을 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // JWT 토큰 기반 인증 시스템에서는 세션 상태를 직접 저장하지 않는 방식이므로 = STATELESS
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 예외 처리 커스텀 핸들러 등록
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                // 인가(Authorization) 부분으로 엔드포인트 접근 권한을 설정하는 부분
                .authorizeHttpRequests((authorize) -> authorize
                        // 1. 인증 없이 접근 가능한 공개 API
                        .requestMatchers("/api/auth/**", "/api/sample/**", "/api/plants/**", "/api/files/**","/uploads/**").permitAll()

                        // 라즈베리파이 -> 서버 전송 (JWT 없이 시리얼 번호로만 인증)
                        .requestMatchers(HttpMethod.POST, "/api/sensor_logs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/photos").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notices/alert").permitAll()

                        // 2. 게시글 '조회'는 로그인 없이도 가능하게 (필요 시 permitAll로 변경 가능)
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()

                        // 3. 게시글 등록(POST), 수정(PUT), 삭제(DELETE) 권한 명시
                        // 특정 게시글(id)에 대한 수정을 위해 경로 패턴을 명확히 합니다.
                        .requestMatchers(HttpMethod.POST, "/api/articles/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**").authenticated()

                        // 4. 댓글 및 좋아요 기능 (인증 필수)
                        .requestMatchers("/api/articles/*/comments/**").authenticated()
                        .requestMatchers("/api/articles/*/likes").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()

                        // 5. 기기 관리 및 나머지 요청
                        .requestMatchers("/api/devices/**").authenticated()
                        .requestMatchers("/api/plants/**").authenticated()
                        .anyRequest().authenticated()
                )
                // 커스텀한 JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                // 이 필터는 요청 헤더의 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
