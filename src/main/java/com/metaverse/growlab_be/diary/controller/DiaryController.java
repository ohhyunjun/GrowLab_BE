package com.metaverse.growlab_be.diary.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/plants/{plantId}/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // Diary 생성 (POST /api/plants/{plantId}/diaries)
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<DiaryResponseDto> createDiary(
            @RequestPart("diaryData") DiaryRequestDto diaryRequestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long userId = principalDetails.user().getId();
        DiaryResponseDto diaryResponseDto = diaryService.createDiary(diaryRequestDto, plantId, userId, files);;
        return ResponseEntity.status(HttpStatus.CREATED).body(diaryResponseDto);
    }

    // 특정 식물의 Diary 전체 조회 (GET /api/plants/{plantId}/diaries)
    @GetMapping()
    public ResponseEntity<List<DiaryResponseDto>> getDiaries(
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        // 현재 로그인한 유저 정보 가져오기
        Long userId = principalDetails.user().getId();
        List<DiaryResponseDto> diaryResponseDtoList = diaryService.getDiariesByPlantId(plantId, userId);
        return ResponseEntity.ok(diaryResponseDtoList);
    }

    // Diary 상세 조회 (GET /api/plants/{plantId}/diaries/{id})
    @GetMapping("/{id}")
    public ResponseEntity<DiaryResponseDto> getDiaryById(
            @PathVariable Long id,
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        // 현재 로그인한 유저 정보 가져오기
        Long userId = principalDetails.user().getId();
        DiaryResponseDto diaryResponseDto = diaryService.getDiaryById(id, plantId, userId);
        return ResponseEntity.ok(diaryResponseDto);
    }

    // Diary 수정 (PUT /api/plants/{plantId}/diaries/{id})
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<DiaryResponseDto> updateDiary(
            @PathVariable Long id,
            @RequestPart("diaryData") DiaryRequestDto diaryRequestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long userId = principalDetails.user().getId();
        DiaryResponseDto diaryResponseDto = diaryService.updateDiary(id, diaryRequestDto, plantId, userId, files);
        return ResponseEntity.ok(diaryResponseDto);
    }

    // Diary 삭제 (DELETE /api/plants/{plantId}/diaries/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long id,
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        // 현재 로그인한 유저 정보 가져오기
        Long userId = principalDetails.user().getId();
        diaryService.deleteDiary(id, plantId, userId);
        return ResponseEntity.noContent().build();
    }
}

