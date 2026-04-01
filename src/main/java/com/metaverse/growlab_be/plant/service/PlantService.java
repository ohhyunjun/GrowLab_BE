package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.dto.PlantResponseDto;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantService {

    private final PlantRepository plantRepository;
    private final SpeciesRepository speciesRepository;

    // 식물 등록하기
    @Transactional
    public PlantResponseDto createPlant(PlantRequestDto plantRequestDto) {
        // 품종 ID로 품종 엔티티 찾기
        Species species = speciesRepository.findById(plantRequestDto.getSpeciesId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품종입니다."));

        // ResquestDto -> Plant 엔티티로 변환
        Plant newPlant = new Plant(plantRequestDto, species);

        // DB에 저장
        Plant savedPlant = plantRepository.save(newPlant);

        // 저장된 Plant 엔티티 -> ResponseDto로 변환
        PlantResponseDto plantResponseDto = new PlantResponseDto(savedPlant);
        return plantResponseDto;
    }

    // 전체 식물 불러오기
    @Transactional(readOnly = true)
    public List<PlantResponseDto> getPlants() {
        List<PlantResponseDto> plantResponseDtoList = plantRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PlantResponseDto::new).toList();
        return plantResponseDtoList;
    }

    // 특정 식물 상세 불러오기
    @Transactional(readOnly = true)
    public PlantResponseDto getPlantById(Long id) {
        Plant foundPlant = findPlantById(id);
        PlantResponseDto plantResponseDto = new PlantResponseDto(foundPlant);
        return plantResponseDto;
    }

    // 특정 식물 정보 수정하기
    @Transactional
    public PlantResponseDto updatePlant(Long id, PlantRequestDto plantRequestDto) {
        // 수정할 식물 엔티티 찾기
        Plant foundPlant = findPlantById(id);
        // 식물 엔티티의 update 메서드 호출하여 수정
        foundPlant.update(plantRequestDto);
        return new PlantResponseDto(foundPlant);
    }

    @Transactional
    public PlantResponseDto updatePlantPartial(Long id, PlantRequestDto plantRequestDto) {
        Plant foundPlant = findPlantById(id);
        foundPlant.update(plantRequestDto);

        return new PlantResponseDto(foundPlant);
    }

    // 특정 식물 삭제하기
    @Transactional
    public void deletePlant(Long id) {
        // 삭제할 식물 엔티티 찾기
        Plant foundPlant = findPlantById(id);
        // 식물 엔티티 삭제
        plantRepository.delete(foundPlant);
    }

    // [공통 메서드] ID로 식물 찾기 및 예외 처리
    private Plant findPlantById(Long id) {
        return plantRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 식물을 찾을 수 없습니다. ID: " + id));
    }
}
