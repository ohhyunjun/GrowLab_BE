package com.metaverse.growlab_be.harvest;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.controller.PhotoController;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.dto.PhotoResponseDto;
import com.metaverse.growlab_be.photo.service.PhotoService;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.plant.service.PlantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP identity verification is deferred, while service-level lifecycle checks remain. */
class DeviceHttpIngressTest {
    final DeviceRepository devices=mock(DeviceRepository.class);
    final PlantRepository plants=mock(PlantRepository.class);
    final PhotoService photos=mock(PhotoService.class);
    final PlantService plantService=mock(PlantService.class);
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc=MockMvcBuilders.standaloneSetup(
                new HarvestController(mock(HarvestService.class),devices,plants),
                new PhotoController(photos,plantService)).build();
        var device=new Device("GROWLAB-G111","GrowLab");
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));
        when(plants.findByDeviceIdAndEndedAtIsNull(device.getId())).thenReturn(List.of());
    }

    @Test void registeredDeviceCanReadEightPortsWithoutDeviceKey() throws Exception {
        mvc.perform(get("/api/device-edge/GROWLAB-G111/ports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serialNumber").value("GROWLAB-G111"))
                .andExpect(jsonPath("$.ports.length()").value(8));
    }

    @Test void unregisteredDeviceStillReturnsNotFound() throws Exception {
        mvc.perform(get("/api/device-edge/UNKNOWN/ports")).andExpect(status().isNotFound());
    }

    @Test void featurePhotoWithoutKeyReachesValidatedService() throws Exception {
        var saved=mock(PhotoResponseDto.class);
        when(saved.getId()).thenReturn(10L);
        when(photos.savePhoto(any())).thenReturn(saved);
        mvc.perform(multipart("/api/photos")
                .file(new MockMultipartFile("imageFile","photo.jpg","image/jpeg",new byte[]{1,2,3}))
                .param("serialNumber","GROWLAB-G111").param("portIndex","0")
                .param("plantId","11").param("capturedAt","2026-10-26T00:00:00Z")
                .param("observationId","f2622c18-72e0-40c2-863e-f0782dd4378a")
                .param("harvestPredictionJson","{\"status\":\"FEATURES_READY\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(10));
        verify(photos).savePhoto(any(PhotoRequestDto.class));
        verifyNoInteractions(plantService);
    }

    @Test void lifecycleConflictIsNotReplacedWithAnAuthenticationError() throws Exception {
        when(photos.savePhoto(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"종료된 재배"));
        mvc.perform(multipart("/api/photos")
                .file(new MockMultipartFile("imageFile","photo.jpg","image/jpeg",new byte[]{1}))
                .param("serialNumber","GROWLAB-G111").param("plantId","11"))
                .andExpect(status().isConflict());
    }
}
