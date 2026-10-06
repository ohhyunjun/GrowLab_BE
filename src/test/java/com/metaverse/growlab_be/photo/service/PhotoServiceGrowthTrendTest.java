package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.GrowthTrendResponseDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PhotoServiceGrowthTrendTest {

    private static final String SERIAL = "GROWLAB-G111";
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 3, 1, 9, 0);

    private PhotoRepository photoRepository;
    private DeviceRepository deviceRepository;
    private PlantRepository plantRepository;
    private PhotoService photoService;
    private Device device;

    @BeforeEach
    void setUp() {
        photoRepository = mock(PhotoRepository.class);
        deviceRepository = mock(DeviceRepository.class);
        plantRepository = mock(PlantRepository.class);
        photoService = new PhotoService(photoRepository, deviceRepository, plantRepository);
        device = new Device(SERIAL, "테스트기기");
        when(deviceRepository.findById(SERIAL)).thenReturn(Optional.of(device));
    }

    @Test
    void 서로_다른_파종일은_같은_경과일로_비교한다() {
        givenPlants(port(0, BASE), port(1, BASE.plusDays(10)),
                port(2, BASE.plusDays(3)), port(3, BASE.plusDays(6)));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 5; day++) {
            photos.add(photo(0, BASE.plusDays(day), 0.20));
            photos.add(photo(1, BASE.plusDays(10 + day), 0.20));
            photos.add(photo(2, BASE.plusDays(3 + day), 0.20));
            photos.add(photo(3, BASE.plusDays(6 + day), 0.20));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(3);
        assertThat(trend.getPoints().get(2).getElapsedDay()).isEqualTo(5L);
        assertThat(trend.getPeerPoints()).hasSize(3);
        assertThat(trend.getPeerPoints().get(2).getValue()).isEqualTo(0.20);
        assertThat(trend.getGapPercent()).isEqualTo(0.0);
        assertThat(trend.getRank()).isEqualTo(1);
        assertThat(trend.getPortCount()).isEqualTo(4);
        assertThat(health(trend, 1)).isEqualTo("good");
    }

    @Test
    void 하루_두포트만_검출돼도_같은_경과일이면_비교한다() {
        givenPlants(port(0, BASE), port(1, BASE));
        givenPhotos(photo(0, BASE.plusDays(5), 0.033), photo(1, BASE.plusDays(5), 0.0094));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getPeerPoints()).hasSize(1);
        assertThat(trend.getPeerPoints().get(0).getValue()).isEqualTo(0.033);
        assertThat(trend.getGapPercent()).isEqualTo(-71.5);
        assertThat(trend.getRank()).isEqualTo(2);
        assertThat(trend.getPortCount()).isEqualTo(2);
        assertThat(health(trend, 0)).isEqualTo("good");
        assertThat(health(trend, 1)).isEqualTo("warn");
    }

    @Test
    void 서로_다른_경과일의_오래된_관측은_끌어오지_않는다() {
        givenPlants(port(0, BASE), port(1, BASE), port(2, BASE), port(3, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 8; day <= 10; day++) photos.add(photo(0, BASE.plusDays(day), 0.30));
        for (int port = 1; port <= 3; port++) {
            for (int day = 6; day <= 8; day++) photos.add(photo(port, BASE.plusDays(day), 0.40));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 0);

        assertThat(trend.getPoints()).hasSize(3);
        assertThat(trend.getPeerPoints()).isEmpty();
        assertThat(trend.getGapPercent()).isNull();
        assertThat(health(trend, 0)).isEqualTo("pending");
    }

    @Test
    void 수확해_비운_포트와_파종_전_사진은_제외한다() {
        givenPlants(port(1, BASE.plusDays(5)));
        givenPhotos(photo(0, BASE.plusDays(8), 0.90),
                photo(1, BASE, 0.01), photo(1, BASE.plusDays(8), 0.12));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getPoints().get(0).getElapsedDay()).isEqualTo(3L);
        assertThat(trend.getLatestRatio()).isEqualTo(0.12);
        assertThat(trend.getGapPercent()).isNull();
        assertThat(health(trend, 1)).isEqualTo("pending");
    }

    @Test
    void 같은_날_여러_고신뢰_사진은_평균으로_묶는다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(4).withHour(9), 0.10),
                photo(1, BASE.plusDays(4).withHour(21), 0.30));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getPoints().get(0).getValue()).isEqualTo(0.20);
    }

    @Test
    void 저신뢰와_미검출_박스는_성장_측정에_포함하지_않는다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(4), 0.10, 0.69, "1"),
                photo(1, BASE.plusDays(5), 0.20, 0.95, "no_detection"));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).isEmpty();
        assertThat(trend.getLatestRatio()).isNull();
        assertThat(health(trend, 1)).isEqualTo("pending");
    }

    private String health(GrowthTrendResponseDto trend, int portIndex) {
        return trend.getPorts().stream().filter(port -> port.getPortIndex() == portIndex)
                .findFirst().orElseThrow().getHealth();
    }

    private void givenPlants(Plant... plants) {
        when(plantRepository.findByDeviceId(SERIAL)).thenReturn(List.of(plants));
    }

    private void givenPhotos(Photo... photos) {
        when(photoRepository.findByDeviceAndBoxAreaRatioIsNotNullOrderByCreatedAtAsc(any(Device.class)))
                .thenReturn(new ArrayList<>(List.of(photos)));
    }

    private Plant port(int portIndex, LocalDateTime plantedAt) {
        Plant plant = new Plant();
        plant.setPortIndex(portIndex);
        plant.setPlantedAt(plantedAt);
        return plant;
    }

    private Photo photo(int portIndex, LocalDateTime shotAt, double ratio) {
        return photo(portIndex, shotAt, ratio, 0.9, "1");
    }

    private Photo photo(int portIndex, LocalDateTime shotAt, double ratio,
                        double confidence, String growthResult) {
        Photo photo = spy(new Photo(device, portIndex, "/p.jpg", "p.jpg",
                growthResult, confidence, "no_detection", 0.0,
                10.0, 10.0, 100.0, ratio, null, 0));
        doReturn(shotAt).when(photo).getCreatedAt();
        return photo;
    }
}
