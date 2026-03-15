package com.metaverse.growlab_be.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, 
                       AccessDeniedException accessDeniedException) 
            throws IOException, ServletException {
        // 로그인이 되었찌만, ROLE_ADMIN이 없어서 막힌 경우 403 처리
        
        // 1. 상태코드 403 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 2. JSON 응답 생성
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", 403);
        responseMap.put("message", "접근 권한이 없습니다. (관리자 권한 필요)");
        responseMap.put("error", "Forbidden");

        // 3. 응답 전송
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
        
    }
}
