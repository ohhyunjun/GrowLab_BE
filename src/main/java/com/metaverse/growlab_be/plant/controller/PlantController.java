package com.metaverse.growlab_be.plant.controller;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.dto.PlantResponseDto;
import com.metaverse.growlab_be.plant.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    // 1. 식물 등록 (POST /api/plants)
    @PostMapping()
    public ResponseEntity<PlantResponseDto> createPlant(
            @RequestBody PlantRequestDto plantRequestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        PlantResponseDto plantResponseDto = plantService.createPlant(plantRequestDto, principalDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(plantResponseDto);
    }

    // 2. 전체 식물 목록 조회 (GET /api/plants)
    @GetMapping()
    public ResponseEntity<List<PlantResponseDto>> getPlants(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<PlantResponseDto> plantResponseDtoList = plantService.getPlants(principalDetails.getUser());
        return ResponseEntity.ok(plantResponseDtoList);
    }

    // 3. 식물 상세 조회 (GET /api/plants/{plantId})
    @GetMapping("/{plantId}")
    public ResponseEntity<PlantResponseDto> getPlantById(
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        PlantResponseDto plantResponseDto = plantService.getPlantById(plantId, principalDetails.getUser());
        return ResponseEntity.ok(plantResponseDto);
    }

    // 4. 식물 정보 전체 수정 (PUT /api/plants/{plantId})
    @PutMapping("/{plantId}")
    public ResponseEntity<PlantResponseDto> updatePlant(
            @PathVariable Long plantId,
            @RequestBody PlantRequestDto plantrequestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        PlantResponseDto plantResponseDto = plantService.updatePlant(plantId, plantrequestDto, principalDetails.getUser());
        return ResponseEntity.ok(plantResponseDto);
    }

    // 5. 식물 정보 부분 수정 (PATCH /api/plants/{plantId})
    @PatchMapping("/{plantId}")
    public ResponseEntity<PlantResponseDto> patchPlant(
            @PathVariable Long plantId,
            @RequestBody PlantRequestDto plantRequestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        PlantResponseDto plantResponseDto = plantService.patchPlant(plantId, plantRequestDto, principalDetails.getUser());
        return ResponseEntity.ok(plantResponseDto);
    }

    // 6. 식물 삭제 (DELETE /api/plants/{plantId})
    @DeleteMapping("/{plantId}")
    public ResponseEntity<Void> deletePlant(
            @PathVariable Long plantId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        plantService.deletePlant(plantId, principalDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
