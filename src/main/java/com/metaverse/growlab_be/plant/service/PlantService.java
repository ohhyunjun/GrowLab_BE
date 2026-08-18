package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.dto.PlantResponseDto;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
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

    @Transactional
    public PlantResponseDto createPlant(PlantRequestDto plantRequestDto, User user) {
        Device device = findDeviceOwnedByUser(plantRequestDto.getSerialNumber(), user);

        if (plantRepository.existsByDeviceIdAndPortIndex(device.getId(), plantRequestDto.getPortIndex())) {
            throw new IllegalArgumentException(plantRequestDto.getPortIndex() + "번 포트에는 이미 식물이 등록되어 있습니다.");
        }

        if (device.getSpecies() == null) {
            throw new IllegalArgumentException("기기에 재배 품종을 먼저 설정해야 합니다.");
        }

        Plant plant = new Plant(plantRequestDto, device);
        // ✅ 요청된 단계가 이 품종의 단계 범위를 벗어나면 안전하게 보정
        plant.setStageIndex(clampStageIndex(device, plantRequestDto.getStageIndex()));

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
        plant.update(plantRequestDto);
        plant.setStageIndex(clampStageIndex(plant.getDevice(), plantRequestDto.getStageIndex()));
        return new PlantResponseDto(plant);
    }

    @Transactional
    public PlantResponseDto patchPlant(Long plantId, PlantRequestDto plantRequestDto, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);

        if (plantRequestDto.getName() != null) plant.setName(plantRequestDto.getName());
        if (plantRequestDto.getStageIndex() != null) {
            plant.setStageIndex(clampStageIndex(plant.getDevice(), plantRequestDto.getStageIndex()));
        }
        if (plantRequestDto.getPlantedAt() != null) plant.setPlantedAt(plantRequestDto.getPlantedAt());

        return new PlantResponseDto(plant);
    }

    @Transactional
    public void deletePlant(Long plantId, User user) {
        Plant plant = findPlantOwnedByUser(plantId, user);
        plantRepository.delete(plant);
    }

    // ✅ 요청된 단계 인덱스를 이 기기의 대표 품종이 가진 단계 범위(0 ~ stageCount-1) 안으로 보정
    private int clampStageIndex(Device device, Integer requested) {
        if (requested == null) return 0;
        int maxIndex = (device != null && device.getSpecies() != null
                ? device.getSpecies().getStageCount()
                : 3) - 1;
        return Math.max(0, Math.min(requested, maxIndex));
    }

    private Plant findPlantOwnedByUser(Long plantId, User user) {
        return plantRepository.findByIdAndUserId(plantId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 식물을 찾을 수 없거나 접근 권한이 없습니다."));
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
