package com.metaverse.growlab_be.species.service;

import com.metaverse.growlab_be.plant.dto.PlantResponseDto;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.dto.SpeciesRequestDto;
import com.metaverse.growlab_be.species.dto.SpeciesResponseDto;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;

    // (관리자) 새 품종 등록
    @Transactional
    public SpeciesResponseDto createSpecies(SpeciesRequestDto speciesrequestDto) {
        // 이름 중복 체크
        speciesRepository.findByName(speciesrequestDto.getName()).ifPresent(s -> {
            throw new IllegalArgumentException("이미 존재하는 품종입니다.");
        });

        // DTO -> Entity 변환 후 저장
        Species newSpecies = new Species(speciesrequestDto);
        Species savedSpecies = speciesRepository.save(newSpecies);

        return new SpeciesResponseDto(savedSpecies);
    }

    // 전체 품종 목록 조회
    @Transactional(readOnly = true)
    public List<SpeciesResponseDto> getAllSpecies() {
        // 최신 등록순으로 정렬하여 조회
        List<SpeciesResponseDto> speciesResponseDtoList = speciesRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SpeciesResponseDto::new).toList();
        return speciesResponseDtoList;
    }

    // (관리자) 품종 삭제
    @Transactional
    public void deleteSpecies(Long id) {
        Species species = findSpeciesById(id);

        // 품종을 참조하는 식물(Plant)이 있으면 삭제 불가
        if (!species.getPlants().isEmpty()) {
            throw new IllegalStateException("해당 품종을 사용하는 식물이 존재하여 삭제할 수 없습니다.");
        }

        speciesRepository.delete(species);
    }

    // [공통 메서드] ID로 품종 찾기
    private Species findSpeciesById(Long id) {
        return speciesRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 품종을 찾을 수 없습니다. ID: " + id));
    }
}
