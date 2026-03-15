package com.metaverse.growlab_be.diary.controller;

import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plants/{plantId}/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // Diary 생성 (POST /api/plants/{plantId}/diaries)
    @PostMapping()
    public ResponseEntity<DiaryResponseDto> createDiary(
            @RequestBody DiaryRequestDto diaryRequestDto,
            @PathVariable Long plantId) {
        DiaryResponseDto diaryResponseDto = diaryService.createDiary(diaryRequestDto, plantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(diaryResponseDto);
    }

    // 특정 식물의 Diary 전체 조회 (GET /api/plants/{plantId}/diaries)
    @GetMapping()
    public ResponseEntity<List<DiaryResponseDto>> getDiaries(
            @PathVariable Long plantId) {
        List<DiaryResponseDto> diaryResponseDtoList = diaryService.getDiaries(plantId);
        return ResponseEntity.ok(diaryResponseDtoList);
    }

    // Diary 상세 조회 (GET /api/plants/{plantId}/diaries/{id})
    @GetMapping("/{id}")
    public ResponseEntity<DiaryResponseDto> getDiaryById(
            @PathVariable Long id,
            @PathVariable Long plantId) {
        DiaryResponseDto diaryResponseDto = diaryService.getDiaryById(id, plantId);
        return ResponseEntity.ok(diaryResponseDto);
    }

    // Diary 수정 (PUT /api/plants/{plantId}/diaries/{id})
    @PutMapping("/{id}")
    public ResponseEntity<DiaryResponseDto> updateDiary(
            @PathVariable Long id,
            @RequestBody DiaryRequestDto diaryRequestDto,
            @PathVariable Long plantId) {
        DiaryResponseDto diaryResponseDto = diaryService.updateDiary(id, diaryRequestDto, plantId);
        return ResponseEntity.ok(diaryResponseDto);
    }

    // Diary 삭제 (DELETE /api/plants/{plantId}/diaries/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long id,
            @PathVariable Long plantId) {
        diaryService.deleteDiary(id, plantId);
        return ResponseEntity.noContent().build();
    }
}

