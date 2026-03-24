package com.metaverse.growlab_be.species.controller;

import com.metaverse.growlab_be.species.dto.SpeciesRequestDto;
import com.metaverse.growlab_be.species.dto.SpeciesResponseDto;
import com.metaverse.growlab_be.species.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    // (관리자) 새 품종 등록 - POST /api/species
    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpeciesResponseDto> createSpecies(@Valid @RequestBody SpeciesRequestDto speciesrequestDto) {
        SpeciesResponseDto speciesresponseDto = speciesService.createSpecies(speciesrequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(speciesresponseDto);
    }

    // 전체 품종 목록 조회 - GET /api/species
    @GetMapping()
    public ResponseEntity<List<SpeciesResponseDto>> getAllSpecies() {
        List<SpeciesResponseDto> speciesResponseDtoList = speciesService.getAllSpecies();
        return ResponseEntity.ok(speciesResponseDtoList);
    }

    // (관리자) 품종 삭제 - DELETE /api/species
    @DeleteMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSpecies(@RequestParam Long speciesId) {
        speciesService.deleteSpecies(speciesId);
        return ResponseEntity.noContent().build();
    }
}
