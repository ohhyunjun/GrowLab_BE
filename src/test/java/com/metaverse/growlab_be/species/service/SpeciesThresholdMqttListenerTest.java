package com.metaverse.growlab_be.species.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.device.service.MqttPublisher;
import com.metaverse.growlab_be.species.domain.Category;
import com.metaverse.growlab_be.species.domain.Difficulty;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.dto.SpeciesRequestDto;
import com.metaverse.growlab_be.species.event.SpeciesThresholdsUpdatedEvent;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeciesThresholdMqttListenerTest {

    @Test
    void speciesUpdatePublishesAfterCommitEvent() {
        SpeciesRepository speciesRepository = mock(SpeciesRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SpeciesService service = new SpeciesService(
                speciesRepository,
                deviceRepository,
                eventPublisher
        );

        Species species = species(7L);
        when(speciesRepository.findById(7L)).thenReturn(Optional.of(species));

        SpeciesRequestDto request = new SpeciesRequestDto();
        ReflectionTestUtils.setField(request, "name", "청상추");
        ReflectionTestUtils.setField(request, "daysToMature", 40);
        ReflectionTestUtils.setField(request, "category", Category.VEGETABLE);
        ReflectionTestUtils.setField(request, "difficulty", Difficulty.EASY);
        ReflectionTestUtils.setField(request, "minTemperature", 19.0);
        ReflectionTestUtils.setField(request, "maxTemperature", 25.0);
        ReflectionTestUtils.setField(request, "minHumidity", 55.0);
        ReflectionTestUtils.setField(request, "maxHumidity", 75.0);
        ReflectionTestUtils.setField(request, "minPh", 5.8);
        ReflectionTestUtils.setField(request, "maxPh", 6.5);
        ReflectionTestUtils.setField(request, "minTds", 500.0);
        ReflectionTestUtils.setField(request, "maxTds", 900.0);
        ReflectionTestUtils.setField(request, "stageNames", List.of("씨앗", "정식기", "생육기", "수확기"));
        ReflectionTestUtils.setField(request, "stageDurationDays", List.of(0, 7, 20, 40));

        service.updateSpecies(7L, request);

        assertThat(species.getMinTemperature()).isEqualTo(19.0);
        assertThat(species.getMaxTds()).isEqualTo(900.0);
        verify(eventPublisher).publishEvent(new SpeciesThresholdsUpdatedEvent(7L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatedThresholdsArePublishedToEveryDeviceUsingSpecies() {
        SpeciesRepository speciesRepository = mock(SpeciesRepository.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        ObjectProvider<MqttPublisher> provider = mock(ObjectProvider.class);
        MqttPublisher mqttPublisher = mock(MqttPublisher.class);
        SpeciesThresholdMqttListener listener = new SpeciesThresholdMqttListener(
                speciesRepository,
                deviceRepository,
                provider
        );

        Species species = species(7L);
        Device first = new Device("GROWLAB-G111", "첫 번째");
        Device second = new Device("GROWLAB-G222", "두 번째");
        when(provider.getIfAvailable()).thenReturn(mqttPublisher);
        when(speciesRepository.findById(7L)).thenReturn(Optional.of(species));
        when(deviceRepository.findBySpecies_Id(7L)).thenReturn(List.of(first, second));

        listener.publishUpdatedThresholds(new SpeciesThresholdsUpdatedEvent(7L));

        verify(mqttPublisher).publishThresholds("GROWLAB-G111", species);
        verify(mqttPublisher).publishThresholds("GROWLAB-G222", species);
    }

    private Species species(Long id) {
        Species species = new Species();
        species.setId(id);
        species.setName("청상추");
        species.setCategory(Category.VEGETABLE);
        species.setDifficulty(Difficulty.EASY);
        species.setStageNames(List.of("씨앗", "정식기", "생육기", "수확기"));
        species.setStageDurationDays(List.of(0, 7, 20, 40));
        return species;
    }
}
