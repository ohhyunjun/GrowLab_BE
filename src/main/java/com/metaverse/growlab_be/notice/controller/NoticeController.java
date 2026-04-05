package com.metaverse.growlab_be.notice.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.notice.dto.NoticeResponseDto;
import com.metaverse.growlab_be.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // (관리자용) 모든 알림 조회
    @GetMapping()
    public ResponseEntity<List<NoticeResponseDto>> getAllNotices(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<NoticeResponseDto> noticeResponseDtoList = noticeService.getAllNotices(principalDetails.user().getId());
        return ResponseEntity.ok(noticeResponseDtoList);
    }

    // 사용자의 읽지 않은 알림 조회
    @GetMapping("/unread")
    public ResponseEntity<List<NoticeResponseDto>> getUnreadNotices(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<NoticeResponseDto> noticeResponseDtoList = noticeService.getUnreadNotices(principalDetails.user().getId());
        return ResponseEntity.ok(noticeResponseDtoList);
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        long count = noticeService.getUnreadCount(principalDetails.user().getId());
        return ResponseEntity.ok(count);
    }

    // 특정 알림 읽음 처리
    @PutMapping("/{noticeId}/read")
    public ResponseEntity<String> readNotice(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            // 1. 서비스 로직 실행
            noticeService.readNotice(noticeId, principalDetails.user().getId());

            // 2. 성공 시 메시지와 함께 200 OK 반환
            return ResponseEntity.ok("알림이 읽음 처리되었습니다.");

        } catch (IllegalArgumentException e) {
            // 3. 존재하지 않는 알림 등 로직 에러 시 400 Bad Request
            return ResponseEntity.badRequest().body("에러: " + e.getMessage());

        } catch (Exception e) {
            // 4. 그 외 예상치 못한 서버 에러 시 500 Internal Server Error
            return ResponseEntity.internalServerError().body("알림 처리 중 서버 오류가 발생했습니다.");
        }
    }

    // 모든 알림 읽음 처리
    @PutMapping("/read-all")
    public ResponseEntity<Void> readAllNotices(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        noticeService.readAllNotices(principalDetails.user().getId());
        return ResponseEntity.ok().build();
    }

    // 특정 알림 삭제
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<String> deleteNotice(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        try {
            // 서비스에서 본인 확인 후 삭제 로직 수행
            noticeService.deleteNotice(noticeId, principalDetails.user().getId());

            // 성공 시 메시지 반환
            return ResponseEntity.ok("알림이 삭제되었습니다.");

        } catch (IllegalArgumentException e) {
            // 해당 알림이 없거나 권한이 없을 때
            return ResponseEntity.badRequest().body("삭제 실패: " + e.getMessage());
        } catch (Exception e) {
            // 서버 내부 오류
            return ResponseEntity.internalServerError().body("알림 삭제 중 오류가 발생했습니다.");
        }
    }
}
