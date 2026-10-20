package com.metaverse.growlab_be.diary.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.diary.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/devices/{serialNumber}/diaries")
@RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponseDto> createDiary(
            @Valid @RequestPart("diaryData") DiaryRequestDto request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @PathVariable String serialNumber,
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                diaryService.createDiary(request, serialNumber, principal.user().getId(), files));
    }

    @GetMapping
    public ResponseEntity<List<DiaryResponseDto>> getDiaries(
            @PathVariable String serialNumber,
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ResponseEntity.ok(diaryService.getDiaries(serialNumber, principal.user().getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiaryResponseDto> getDiaryById(
            @PathVariable String serialNumber, @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ResponseEntity.ok(diaryService.getDiaryById(id, serialNumber, principal.user().getId()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponseDto> updateDiary(
            @PathVariable String serialNumber, @PathVariable Long id,
            @Valid @RequestPart("diaryData") DiaryRequestDto request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails principal) {
        return ResponseEntity.ok(diaryService.updateDiary(id, request, serialNumber, principal.user().getId(), files));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable String serialNumber, @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principal) {
        diaryService.deleteDiary(id, serialNumber, principal.user().getId());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalidDiary(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(error.getMessage());
    }
}
