package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.diary.service.DiaryService;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.domain.PlantEndReason;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiaryServicePlantTest {

    @Test
    void aDiaryUsesTheSelectedCultivationIdAndRejectsAnEndedPlant() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        Device device = new Device("GROWLAB-G111", "test");
        device.setUser(user);
        Species species = new Species();
        species.setName("딸기");
        Plant oldPlant = plant(7L, device, species);
        oldPlant.finish(PlantEndReason.DIED);
        Plant newPlant = plant(8L, device, species);

        UserRepository users = mock(UserRepository.class);
        DeviceRepository devices = mock(DeviceRepository.class);
        PlantRepository plants = mock(PlantRepository.class);
        DiaryRepository diaries = mock(DiaryRepository.class);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));
        when(plants.findById(7L)).thenReturn(Optional.of(oldPlant));
        when(plants.findById(8L)).thenReturn(Optional.of(newPlant));
        when(diaries.save(any(Diary.class))).thenAnswer(call -> call.getArgument(0));
        DiaryService service = new DiaryService(diaries, devices, plants, users, mock(FileService.class));

        assertThatThrownBy(() -> service.createDiary(request(7L), device.getId(), 5L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.createDiary(request(8L), device.getId(), 5L, null).getPlantId())
                .isEqualTo(8L);
    }

    private Plant plant(Long id, Device device, Species species) {
        Plant plant = new Plant();
        plant.setId(id);
        plant.setDevice(device);
        plant.setSpecies(species);
        plant.setPortIndex(1);
        plant.setActivePortIndex(1);
        plant.setName("베리");
        return plant;
    }

    private DiaryRequestDto request(Long plantId) {
        DiaryRequestDto request = new DiaryRequestDto();
        ReflectionTestUtils.setField(request, "title", "기록");
        ReflectionTestUtils.setField(request, "content", "상태 확인");
        ReflectionTestUtils.setField(request, "targetDate", LocalDateTime.now());
        ReflectionTestUtils.setField(request, "plantId", plantId);
        return request;
    }
}
