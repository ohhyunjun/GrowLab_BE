package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.device.service.MqttPublisher;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.notice.service.NoticeService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.domain.PlantEndReason;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlantLifecycleTest {

    @Test
    void endingAPlantFreesItsPortButKeepsTheDiaryLinkedToThatCultivation() {
        Device device = new Device("GROWLAB-G111", "test");
        device.setPortStatus("01000000");
        Species species = new Species();
        species.setName("딸기");
        Plant oldPlant = new Plant();
        oldPlant.setId(7L);
        oldPlant.setDevice(device);
        oldPlant.setSpecies(species);
        oldPlant.setPortIndex(1);
        oldPlant.setActivePortIndex(1);
        oldPlant.setName("베리");
        device.getPlants().add(oldPlant);

        DiaryRequestDto request = new DiaryRequestDto();
        ReflectionTestUtils.setField(request, "title", "첫 재배");
        ReflectionTestUtils.setField(request, "content", "성장 기록");
        ReflectionTestUtils.setField(request, "targetDate", LocalDateTime.now());
        Diary diary = new Diary(request, device, mock(User.class), oldPlant);

        PlantRepository plants = mock(PlantRepository.class);
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(5L);
        when(plants.findByIdAndUserId(7L, 5L)).thenReturn(Optional.of(oldPlant));
        PlantService service = new PlantService(plants, mock(DiaryRepository.class),
                mock(DeviceRepository.class), mock(SpeciesRepository.class),
                mock(NoticeService.class), mock(ObjectProvider.class));

        service.finishPlant(7L, PlantEndReason.HARVESTED, owner);

        assertThat(oldPlant.getEndReason()).isEqualTo(PlantEndReason.HARVESTED);
        assertThat(oldPlant.getEndedAt()).isNotNull();
        assertThat(oldPlant.getActivePortIndex()).isNull();
        assertThat(device.getPortStatus()).isEqualTo("00000000");
        assertThat(device.getActivePlants()).isEmpty();
        assertThat(diary.getPlant()).isSameAs(oldPlant);
        assertThat(diary.getPlantName()).isEqualTo("베리");
        verify(plants, never()).delete(oldPlant);
    }
}
