package com.metaverse.growlab_be.plant.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.device.service.MqttPublisher;
import com.metaverse.growlab_be.notice.service.NoticeService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.dto.PlantRequestDto;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlantServiceObservationTest {

    private PlantRepository plantRepository;
    private DeviceRepository deviceRepository;
    private SpeciesRepository speciesRepository;
    private PlantService plantService;

    @BeforeEach
    void setUp() {
        plantRepository = mock(PlantRepository.class);
        deviceRepository = mock(DeviceRepository.class);
        speciesRepository = mock(SpeciesRepository.class);
        plantService = new PlantService(plantRepository, deviceRepository, speciesRepository,
                mock(NoticeService.class), mock(ObjectProvider.class));
    }

    @Test
    void ignoresDiseaseBelowSixtyPercent() {
        Plant plant = configuredPlant(null);
        plant.setDiseaseResult("previous_result");

        plantService.applyObservation("GROWLAB-G111", 0,
                "no_detection", 0.0, "leaf_spot", 0.59);

        assertThat(plant.getDiseaseResult()).isEqualTo("previous_result");
        verify(plantRepository, never()).save(any());
    }

    @Test
    void acceptsAnyDiseaseLabelAtSixtyPercent() {
        Plant plant = configuredPlant(null);

        plantService.applyObservation("GROWLAB-G111", 0,
                "no_detection", 0.0, "leaf_spot", 0.60);

        assertThat(plant.getDiseaseResult()).isEqualTo("leaf_spot");
        verify(plantRepository).save(plant);
    }

    @Test
    void ignoresGrowthBelowSeventyPercent() {
        Plant plant = configuredPlant(speciesWithFourStages());

        plantService.applyObservation("GROWLAB-G111", 0,
                "2", 0.69, "no_detection", 0.0);

        assertThat(plant.getStageIndex()).isZero();
        verify(plantRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"2, 2", "growth, 2", "harvest, 3"})
    void mapsNumericAndCanonicalGrowthLabels(String result, int expectedStage) {
        Plant plant = configuredPlant(speciesWithFourStages());

        plantService.applyObservation("GROWLAB-G111", 0,
                result, 0.70, "no_detection", 0.0);

        assertThat(plant.getStageIndex()).isEqualTo(expectedStage);
        verify(plantRepository).save(plant);
    }

    @Test
    void createsDifferentSpeciesOnDifferentPortsOfOneDevice() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Device device = new Device("GROWLAB-G111", "test");
        device.setUser(user);
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        Species strawberry = speciesWithFourStages();
        strawberry.setId(1L);
        strawberry.setName("딸기");
        Species lettuce = speciesWithFourStages();
        lettuce.setId(2L);
        lettuce.setName("상추");
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(strawberry));
        when(speciesRepository.findById(2L)).thenReturn(Optional.of(lettuce));
        when(plantRepository.saveAndFlush(any(Plant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlantRequestDto first = requestForPort(0, 1L);
        PlantRequestDto second = requestForPort(1, 2L);
        plantService.createPlant(first, user);
        plantService.createPlant(second, user);

        ArgumentCaptor<Plant> saved = ArgumentCaptor.forClass(Plant.class);
        verify(plantRepository, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues()).extracting(Plant::getPortIndex).containsExactly(0, 1);
        assertThat(saved.getAllValues()).extracting(plant -> plant.getSpecies().getId()).containsExactly(1L, 2L);
    }

    private PlantRequestDto requestForPort(int portIndex, long speciesId) {
        PlantRequestDto request = new PlantRequestDto();
        ReflectionTestUtils.setField(request, "name", "test");
        ReflectionTestUtils.setField(request, "plantedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(request, "serialNumber", "GROWLAB-G111");
        ReflectionTestUtils.setField(request, "portIndex", portIndex);
        ReflectionTestUtils.setField(request, "speciesId", speciesId);
        return request;
    }

    @Test
    void growthStageUsesTheSpeciesAssignedToThePort() {
        Plant plant = configuredPlant(speciesWithFourStages());
        plant.setSpecies(speciesWithFourStages());

        plantService.applyObservation("GROWLAB-G111", 0,
                "harvest", 0.70, "no_detection", 0.0);

        assertThat(plant.getStageIndex()).isEqualTo(3);
    }

    private Plant configuredPlant(Species species) {
        Device device = new Device("GROWLAB-G111", "test");
        device.setUser(mock(User.class));
        Plant plant = new Plant();
        plant.setSpecies(species);
        plant.setPortIndex(0);
        plant.setStageIndex(0);
        plant.setDevice(device);

        when(deviceRepository.findById("GROWLAB-G111")).thenReturn(Optional.of(device));
        when(plantRepository.findByDeviceIdAndPortIndex("GROWLAB-G111", 0))
                .thenReturn(Optional.of(plant));
        return plant;
    }

    private Species speciesWithFourStages() {
        Species species = new Species();
        species.setStageNames(List.of("씨앗", "정식기", "생육기", "수확기"));
        return species;
    }
}
