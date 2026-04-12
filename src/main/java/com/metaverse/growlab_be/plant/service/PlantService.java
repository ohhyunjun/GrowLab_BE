package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.dto.PlantResponseDto;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantService {

    private final PlantRepository plantRepository;
    private final DeviceRepository deviceRepository;
    private final SpeciesRepository speciesRepository;

    @Transactional
    public PlantResponseDto createPlant(PlantRequestDto plantRequestDto, User user) {
        Device device = findDeviceOwnedByUser(plantRequestDto.getSerialNumber(), user);

        if (plantRepository.existsByDeviceId(device.getId())) {
            throw new IllegalArgumentException("해당 디바이스에는 이미 식물이 등록되어 있습니다.");
        }

        Species species = findSpeciesById(plantRequestDto.getSpeciesId());

        Plant plant = new Plant(
                plantRequestDto,
                species,
                device
        );

        Plant savedPlant = plantRepository.save(plant);
        return new PlantResponseDto(savedPlant);
    }

    public List<PlantResponseDto> getPlants(User user) {
        List<Plant> plants = plantRepository.findByUserIdOrderByPlantedAtAsc(user.getId());
        return plants.stream()
                .map(PlantResponseDto::new)
                .toList();
    }

    public PlantResponseDto getPlantById(Long plantId, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        return new PlantResponseDto(plant);
    }

    @Transactional
    public PlantResponseDto updatePlant(Long plantId, PlantRequestDto plantRequestDto, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        Species species = findSpeciesById(plantRequestDto.getSpeciesId());
        plant.setSpecies(species);

        plant.update(plantRequestDto);

        return new PlantResponseDto(plant);
    }

    @Transactional
    public PlantResponseDto patchPlant(Long plantId, PlantRequestDto plantRequestDto, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);

        if (plantRequestDto.getName() != null) {
            plant.setName(plantRequestDto.getName());
        }
        if (plantRequestDto.getSpeciesId() != null) {
            Species species = findSpeciesById(plantRequestDto.getSpeciesId());
            plant.setSpecies(species);
        }
        if (plantRequestDto.getPlantStage() != null) {
            plant.setPlantStage(plantRequestDto.getPlantStage());
        }
        if (plantRequestDto.getPlantedAt() != null) {
            plant.setPlantedAt(plantRequestDto.getPlantedAt());
        }
        return new PlantResponseDto(plant);
    }

    @Transactional
    public void deletePlant(Long plantId, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        plantRepository.delete(plant);
    }

    // 헬퍼 메서드들 (이하 동일)
    private Plant findPlantOwnedByUser(Long plantId, User user) {
        return plantRepository.findByIdAndUserId(plantId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 식물을 찾을 수 없거나 접근 권한이 없습니다."));
    }

    private Species findSpeciesById(Long speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품종입니다: " + speciesId));
    }

    private Device findDeviceOwnedByUser(String deviceSerial, User user) {
        Device device = deviceRepository.findById(deviceSerial)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 디바이스입니다: " + deviceSerial));

        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 디바이스에 대한 권한이 없습니다.");
        }
        return device;
    }
}
