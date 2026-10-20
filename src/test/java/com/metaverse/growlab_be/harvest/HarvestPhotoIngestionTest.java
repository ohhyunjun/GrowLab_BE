package com.metaverse.growlab_be.harvest;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.photo.service.PhotoService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.plant.service.PlantService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HarvestPhotoIngestionTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path photoDirectory;
    @Test void storesPythonResultAndPhotoInOneIngestion() throws Exception {
        var photos=mock(PhotoRepository.class);var devices=mock(DeviceRepository.class);var plants=mock(PlantRepository.class);
        var predictions=mock(HarvestPredictionRepository.class);
        var inference=mock(HarvestInferenceClient.class);
        when(inference.predict(any())).thenAnswer(invocation->invocation.getArgument(0));
        var harvest=new HarvestService(predictions,plants,devices,inference);var observations=mock(PlantService.class);
        var service=new PhotoService(photos,devices,plants,harvest,observations);
        var device=new Device("TEST","Test");device.setPortStatus("10000000");
        var species=new com.metaverse.growlab_be.species.domain.Species();species.setName("상추");
        var plant=new Plant();plant.setId(1L);plant.setDevice(device);plant.setPortIndex(0);plant.setSpecies(species);
        String fixture=new String(getClass().getResourceAsStream("/edge_harvest_fixture.json").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().readValue(fixture,HarvestPayload.class);
        var dto=new PhotoRequestDto();dto.setSerialNumber("TEST");dto.setPortIndex(0);dto.setPlantId(1L);
        dto.setCapturedAt(payload.capturedAt().toString());dto.setObservationId(payload.observationId());dto.setHarvestPredictionJson(fixture);
        dto.setImageFile(new MockMultipartFile("imageFile","x.jpg","image/jpeg",new byte[]{1}));
        when(devices.findLockedById("TEST")).thenReturn(Optional.of(device));when(plants.findLockedById(1L)).thenReturn(Optional.of(plant));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"uploadDir",photoDirectory.toString());
        when(photos.save(any())).thenAnswer(i->{Photo p=i.getArgument(0);org.springframework.test.util.ReflectionTestUtils.setField(p,"id",5L);return p;});
        var result=service.savePhoto(dto);
        verify(inference).predict(any());
        try (var saved=java.nio.file.Files.list(photoDirectory)) { assertThat(saved.count()).isEqualTo(1); }
        assertThat(result.getPlantId()).isEqualTo(1L);assertThat(result.getCapturedAt()).isEqualTo(payload.capturedAt());
        var captor=org.mockito.ArgumentCaptor.forClass(HarvestPrediction.class);verify(predictions).save(captor.capture());
        assertThat(captor.getValue().getPhotoId()).isEqualTo(5L);assertThat(captor.getValue().getFeaturesJson()).isNotBlank();
        verify(observations).applyObservationForPlant(eq(plant),eq("2"),eq(1.),anyString(),anyDouble());
    }

    @Test void networkRetryReturnsOriginalPhotoWithoutDuplicatingStorageOrApplyingStageAgain() throws Exception {
        var photos=mock(PhotoRepository.class);var devices=mock(DeviceRepository.class);var plants=mock(PlantRepository.class);
        var harvest=mock(HarvestService.class);var observations=mock(PlantService.class);
        var service=new PhotoService(photos,devices,plants,harvest,observations);
        var device=new Device("TEST","Test");var plant=new Plant();plant.setId(1L);
        Instant t=Instant.parse("2026-10-01T00:00:00Z");
        var dto=new PhotoRequestDto();dto.setSerialNumber("TEST");dto.setPortIndex(0);dto.setPlantId(1L);
        dto.setCapturedAt(t.toString());dto.setObservationId("same");dto.setHarvestPredictionJson("{}");
        dto.setImageFile(new MockMultipartFile("imageFile","x.jpg","image/jpeg",new byte[]{1}));
        var original=new Photo(device,0,"image.jpg","image.jpg","2",.9,"Normal",.8);
        original.bindCapture(1L,t,"same");
        when(devices.findLockedById("TEST")).thenReturn(Optional.of(device));
        when(harvest.validateCultivation(device,0,1L,t)).thenReturn(plant);
        when(photos.findByDeviceIdAndObservationId("TEST","same")).thenReturn(Optional.of(original));
        assertThat(service.savePhoto(dto).getObservationId()).isEqualTo("same");
        verifyNoInteractions(observations);
        try (var saved=java.nio.file.Files.list(photoDirectory)) { assertThat(saved.count()).isZero(); }
        verify(photos,never()).save(any());
    }
}
