package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.file.service.S3StorageService;
import com.metaverse.growlab_be.harvest.HarvestService;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.plant.service.PlantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PhotoTrackingUploadTest {
    @TempDir Path directory;

    @Test
    void 새_사진의_녹색_측정도_이전_재배의_전체_범위를_따라가고_영역을_기록한다() throws Exception {
        var photos = mock(PhotoRepository.class);
        var devices = mock(DeviceRepository.class);
        var plants = mock(PlantRepository.class);
        var service = new PhotoService(photos, devices, plants, mock(S3StorageService.class),
                mock(HarvestService.class), mock(PlantService.class));
        ReflectionTestUtils.setField(service, "uploadDir", directory.toString());
        ReflectionTestUtils.setField(service, "cameraChangeSerial", "GROWLAB-G111");
        ReflectionTestUtils.setField(service, "cameraChangeAt", "2026-10-19T18:00:00");
        var device = new Device("GROWLAB-G111", "test");
        var plant = new Plant(); plant.setId(42L); plant.setPortIndex(1);
        plant.setPlantedAt(LocalDateTime.of(2026, 10, 20, 9, 0));
        var previous = mock(Photo.class);
        when(previous.getId()).thenReturn(1L); when(previous.getPortIndex()).thenReturn(1);
        when(previous.getDevice()).thenReturn(device); when(previous.getPlantId()).thenReturn(42L);
        when(previous.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 10, 27, 9, 0));
        when(previous.getGrowthStageResult()).thenReturn("1"); when(previous.getConfidence()).thenReturn(.9);
        when(previous.getDetectionsJson()).thenReturn("""
                [{"conf":0.9,"x":0.5,"y":0.5,"w":0.2,"h":0.3}]
                """);
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));
        when(plants.findByDeviceIdAndPortIndexAndEndedAtIsNull(device.getId(), 1)).thenReturn(Optional.of(plant));
        when(photos.findTop100ByDeviceAndPortIndexOrderByCreatedAtDesc(device, 1)).thenReturn(List.of(previous));
        when(photos.save(any())).thenAnswer(call -> {
            Photo photo = call.getArgument(0); ReflectionTestUtils.setField(photo, "id", 2L); return photo;
        });
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 36; y < 64; y++) for (int x = 66; x < 84; x++) image.setRGB(x, y, 0x40bb30);
        var output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output);
        var dto = new PhotoRequestDto(); dto.setSerialNumber(device.getId()); dto.setPortIndex(1);
        dto.setGrowthResult("1"); dto.setGrowthConfidence(.9); dto.setBoxAreaRatio(.06);
        dto.setImageFile(new MockMultipartFile("imageFile", "plant.png", "image/png", output.toByteArray()));
        dto.setDetectionsJson("[{\"conf\":0.9,\"x\":0.75,\"y\":0.5,\"w\":0.2,\"h\":0.3}]");
        service.savePhoto(dto);
        var saved = org.mockito.ArgumentCaptor.forClass(Photo.class); verify(photos).save(saved.capture());
        assertThat(saved.getValue().getGreenAreaRatio()).isGreaterThan(.045);
        assertThat(saved.getValue().getGreenAreaRegion()).startsWith("tracked-v2:");
    }
}
