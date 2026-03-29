package com.metaverse.growlab_be.auth.filter;

import com.metaverse.growlab_be.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil; // JWT 토큰 생성 및 검증 로직 주입
    private final UserDetailsService userDetailsService; //  인증사용자 정볼르 로드할 UserDetails 주입

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. HTTP 요청 헤더에서 Authorization 이름의 키 값을 가져옴(JWT 토큰값)
        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String username;

        // 2. Authorization 헤더가 없거나, "Bearer "로 시작되지 않는 경우,
        // JWT가 없는 요청으로 간주(인증되지 않은 상태) 다음 필터로 스킵
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 접두사(7자리) 제거하고 순수 JWT 토큰 추출
        jwtToken = authHeader.substring(7);
        try {
            // 3-1. JWT 토큰에서 사용자 계정(Username) 추출
            username = jwtUtil.extractUsername(jwtToken);
        } catch (Exception e) {
            // 3-2. JWT 토큰 파싱 및 검증 오류 발생 시 Unauthorized 예외 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(e.getMessage());
            return;
        }
        
        // 4. JWT 토큰에서 사용자 계정(Username)이 존재하며, SpringSecurityContext공간에 인증 정보가 존재하지 않는 경우에만 인증 진행
        if (username != null || SecurityContextHolder.getContext().getAuthentication() == null) {
            // 5. UserDetailsService통해 사용자계정(Username)을 기반으로 UserDetails 객체 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // 6. 토큰 유효성 검사(사용자 이름 일치, 토큰 만료 여부)
            if (jwtUtil.validateToken(jwtToken, userDetails)) {
                // 7. 토큰이 유효한 경우, Spring SecurityContext에 인증 정보 설정
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // 인증된 상요자 객체(UserDetails)
                        null, // 자격증명은 이미 토큰으로 검증되었기 때문에 null로 설정
                        userDetails.getAuthorities() // 사용자 권한 목록중 하나
                );

                // 8. 요청에 대한 웹 인증 세부 정보(IP, 세션 ID 등) 설정
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. SecurityContextHolder에 생성된 인증 토큰 설정
                SecurityContextHolder.getContext().setAuthentication(authToken);
                // -> 현재 요청에 대해 인증된 사용자임을 Spring Security에 알림
            }
            // 10. 필터 체인 계속 진행
            filterChain.doFilter(request, response);
        }
    }
}
