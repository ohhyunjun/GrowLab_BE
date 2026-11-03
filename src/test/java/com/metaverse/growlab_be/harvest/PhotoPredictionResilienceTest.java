package com.metaverse.growlab_be.harvest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.file.service.S3StorageService;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.photo.service.PhotoService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.plant.service.PlantService;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class PhotoPredictionResilienceTest {
    @Test void ec2OutageStillStoresPhotoAndPendingInputs() throws Exception {
        var photos=mock(PhotoRepository.class);var devices=mock(DeviceRepository.class);
        var plants=mock(PlantRepository.class);var predictions=mock(HarvestPredictionRepository.class);
        var inference=mock(HarvestInferenceClient.class);var storage=mock(S3StorageService.class);
        var harvest=new HarvestService(predictions,plants,devices,inference);
        var service=new PhotoService(photos,devices,plants,storage,harvest,mock(PlantService.class));
        Device device=new Device("TEST","test");device.setPortStatus("10000000");
        Species species=new Species();species.setName("청상추");
        Plant plant=new Plant();plant.setId(1L);plant.setDevice(device);plant.setPortIndex(0);plant.setSpecies(species);
        when(devices.findLockedById("TEST")).thenReturn(Optional.of(device));
        when(plants.findLockedById(1L)).thenReturn(Optional.of(plant));
        when(inference.predict(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        when(storage.isEnabled()).thenReturn(true);
        when(storage.upload(any(),any())).thenReturn(new S3StorageService.UploadResult("fixture.jpg","saved.jpg","uploads/camera/saved.jpg","https://example.invalid/saved.jpg"));
        when(photos.save(any())).thenAnswer(call->{Object p=call.getArgument(0);ReflectionTestUtils.setField(p,"id",123L);return p;});
        Instant time=Instant.now();String observation=UUID.randomUUID().toString();
        var payload=new HarvestPayload(1,observation,time,time,HarvestService.MODEL,HarvestService.FEATURES,
                "FEATURES_READY","growing",time,null,null,new ArrayList<>(Collections.nCopies(275,0.)),2);
        PhotoRequestDto dto=new PhotoRequestDto();dto.setSerialNumber("TEST");dto.setPortIndex(0);dto.setPlantId(1L);
        dto.setCapturedAt(time.toString());dto.setObservationId(observation);
        dto.setHarvestPredictionJson(new ObjectMapper().findAndRegisterModules().writeValueAsString(payload));
        dto.setImageFile(new MockMultipartFile("imageFile","fixture.jpg","image/jpeg",new byte[]{1,2,3}));
        assertThat(service.savePhoto(dto).getId()).isEqualTo(123L);
        verify(storage).upload(any(),any());verify(photos).save(any());
        var saved=ArgumentCaptor.forClass(HarvestPrediction.class);verify(predictions).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo("FEATURES_READY");
        assertThat(saved.getValue().getFeaturesJson()).isNotBlank();
    }
}
