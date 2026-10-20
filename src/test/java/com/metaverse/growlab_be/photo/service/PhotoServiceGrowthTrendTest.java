package com.metaverse.growlab_be.photo.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.GrowthTrendResponseDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
        photoService = new PhotoService(photoRepository, deviceRepository, plantRepository,
                mock(com.metaverse.growlab_be.harvest.HarvestService.class), mock(com.metaverse.growlab_be.plant.service.PlantService.class));
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
        // 크기 차이만으로는 포트 상태를 정하지 않는다. 속도를 낼 만큼 관측이 쌓이기 전에는 대기다.
        assertThat(health(trend, 0)).isEqualTo("pending");
        assertThat(health(trend, 1)).isEqualTo("pending");
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
        // 크기는 견주지 않지만, 같은 시기(4~10일차)의 속도는 세 포트 모두 관측이 있어 견줄 수 있다.
        assertThat(trend.getReference().getVerdict()).isEqualTo("similar");
        assertThat(health(trend, 0)).isEqualTo("good");
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
    void 같은_날_여러_고신뢰_사진은_중앙값으로_묶는다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(4).withHour(9), 0.10),
                photo(1, BASE.plusDays(4).withHour(21), 0.30),
                photo(1, BASE.plusDays(5).withHour(3), 0.20), photo(1, BASE.plusDays(5).withHour(9), 0.22),
                photo(1, BASE.plusDays(5).withHour(15), 0.02));   // 잎만 잡힌 한 장

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(2);
        assertThat(trend.getPoints().get(0).getValue()).isEqualTo(0.20);
        assertThat(trend.getPoints().get(1).getValue()).isEqualTo(0.20);
    }

    @Test
    void 식물과_떨어진_큰_배경_박스는_크기로_쓰지_않는다() {
        givenPlants(port(1, BASE));
        givenPhotos(photoWithDetections(1, BASE.plusDays(4), 0.345, """
                [{"cls":"Planting","conf":0.96,"x":0.25,"y":0.60,"w":0.25,"h":0.32},
                 {"cls":"Planting","conf":0.85,"x":0.20,"y":0.62,"w":0.10,"h":0.12},
                 {"cls":"Growing","conf":0.34,"x":0.77,"y":0.51,"w":0.42,"h":0.81}]"""));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getLatestRatio()).isEqualTo(0.08);
    }

    @Test
    void 광각_사진에서_옆_포트_식물이_더_확실해도_촬영_포트_박스를_쓴다() {
        ReflectionTestUtils.setField(photoService, "cameraChangeSerial", SERIAL);
        ReflectionTestUtils.setField(photoService, "cameraChangeAt", "2026-10-19T18:00:00");
        LocalDateTime planted = LocalDateTime.of(2026, 10, 19, 9, 0);
        givenPlants(port(1, planted));
        givenPhotos(photoWithDetections(1, planted.plusDays(1), .036, """
                [{"conf":0.98,"x":0.87,"y":0.40,"w":0.18,"h":0.20},
                 {"conf":0.73,"x":0.39,"y":0.47,"w":0.10,"h":0.25}]"""));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);
        assertThat(trend.getLatestRatio()).isEqualTo(.025);
    }

    @Test
    void 광각_사진에_옆_포트_식물만_있으면_성장_관측을_남기지_않는다() {
        ReflectionTestUtils.setField(photoService, "cameraChangeSerial", SERIAL);
        ReflectionTestUtils.setField(photoService, "cameraChangeAt", "2026-10-19T18:00:00");
        LocalDateTime planted = LocalDateTime.of(2026, 10, 19, 9, 0);
        givenPlants(port(1, planted));
        givenPhotos(photoWithDetections(1, planted.plusDays(1), .036, """
                [{"conf":0.98,"x":0.87,"y":0.40,"w":0.18,"h":0.20}]"""));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);
        assertThat(trend.getPoints()).isEmpty();
        assertThat(trend.getLatestRatio()).isNull();
    }

    @Test
    void 광각_사진의_중앙_새싹은_신뢰도_0점6대도_쓰고_0점3대는_제외한다() {
        ReflectionTestUtils.setField(photoService, "cameraChangeSerial", SERIAL);
        ReflectionTestUtils.setField(photoService, "cameraChangeAt", "2026-10-19T18:00:00");
        LocalDateTime planted = LocalDateTime.of(2026, 10, 19, 9, 0);
        givenPlants(port(4, planted));
        givenPhotos(photoWithDetections(4, planted.plusDays(1), .03037, .6475, """
                [{"conf":0.6475,"x":0.60216,"y":0.61458,"w":0.26322,"h":0.11538},
                 {"conf":0.4006,"x":0.6863,"y":0.61058,"w":0.09195,"h":0.07572},
                 {"conf":0.3784,"x":0.59796,"y":0.63061,"w":0.03816,"h":0.08654}]"""),
                photoWithDetections(4, planted.plusDays(2), .0039, .3596, """
                [{"conf":0.3596,"x":0.80349,"y":0.40705,"w":0.05048,"h":0.07732}]"""));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 4);
        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getLatestRatio()).isEqualTo(.0308);
    }

    @Test
    void 화면_끝에_닿은_사진도_최소_크기로_남긴다() {
        givenPlants(port(1, BASE));
        givenPhotos(photoWithDetections(1, BASE.plusDays(4), 0.04, """
                [{"cls":"Planting","conf":0.92,"x":0.44,"y":0.87,"w":0.16,"h":0.26}]"""),
                photoWithDetections(1, BASE.plusDays(5), 0.09, """
                [{"cls":"Planting","conf":0.96,"x":0.32,"y":0.61,"w":0.30,"h":0.30}]"""));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(2);
        assertThat(trend.getPoints().get(0).getElapsedDay()).isEqualTo(4L);
        assertThat(trend.getPoints().get(0).isLowerBound()).isTrue();
        assertThat(trend.getLatestRatio()).isEqualTo(0.09);
        assertThat(trend.getSizeChange().isReady()).isFalse();
    }

    @Test
    void 잘린_최근_사진은_확인된_증가율을_최소값으로_표시한다() {
        givenPlants(port(1, BASE));
        givenPhotos(photoWithDetections(1, BASE.plusDays(4), 0.64, """
                        [{"cls":"Growing","conf":0.93,"x":0.5,"y":0.5,"w":0.8,"h":0.8}]"""),
                photoWithDetections(1, BASE.plusDays(5).withHour(9), 0.6724, """
                        [{"cls":"Growing","conf":0.93,"x":0.5,"y":0.5,"w":0.82,"h":0.82}]"""),
                photoWithDetections(1, BASE.plusDays(5).withHour(17), 0.81, """
                        [{"cls":"Growing","conf":0.93,"x":0.5,"y":0.45,"w":0.9,"h":0.9}]"""));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(2);
        assertThat(trend.getLatestRatio()).isEqualTo(0.7412);
        assertThat(trend.isLatestLowerBound()).isTrue();
        assertThat(trend.getSizeChange().isReady()).isTrue();
        assertThat(trend.getSizeChange().isLowerBound()).isTrue();
        assertThat(trend.getSizeChange().getChangePercent()).isEqualTo(15.8);
    }

    @Test
    void 녹색_측정이_두_날에_있으면_통합_크기를_쓴다() {
        givenPlants(port(1, BASE));
        givenPhotos(photoWithGreen(1, BASE.plusDays(4), .20, .05),
                photoWithGreen(1, BASE.plusDays(5), .30, .10));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getMeasurement()).isEqualTo("box_green");
        assertThat(trend.getLatestRatio()).isEqualTo(Math.round(VisualSizeIndex.combine(.30, .10) * 10000) / 10000.0);
        assertThat(trend.getLatestBoxRatio()).isEqualTo(.30);
        assertThat(trend.getLatestGreenRatio()).isEqualTo(.10);
    }

    @Test
    void 통합_속도를_박스만_있는_다른_재배의_속도와_섞지_않는다() {
        givenPlants(port(0, BASE), port(1, BASE), port(2, BASE), port(3, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 6; day++) {
            photos.add(photoWithGreen(0, BASE.plusDays(day), .10 * Math.pow(1.10, day - 3),
                    .04 * Math.pow(1.10, day - 3)));
            for (int port = 1; port <= 3; port++)
                photos.add(photo(port, BASE.plusDays(day), .10 * Math.pow(1.10, day - 3)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 0);

        assertThat(trend.getMeasurement()).isEqualTo("box_green");
        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(10.0);
        assertThat(trend.getReference().isReady()).isFalse();
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

    @Test
    void 다른_품종의_포트는_성장_또래와_건강_판정에서_제외한다() {
        givenPlants(port(0, BASE, 1L), port(1, BASE, 2L));
        givenPhotos(photo(0, BASE.plusDays(5), 0.30),
                photo(1, BASE.plusDays(5), 0.01));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getPeerPoints()).isEmpty();
        assertThat(trend.getGapPercent()).isNull();
        assertThat(trend.getRank()).isNull();
        assertThat(health(trend, 0)).isEqualTo("pending");
        assertThat(health(trend, 1)).isEqualTo("pending");
    }

    @Test
    void 혼자_심은_포트도_최근_관측으로_하루_성장률을_낸다() {
        givenPlants(port(1, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 7; day++) photos.add(photo(1, BASE.plusDays(day), 0.05 * Math.pow(1.10, day - 3)));
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("growing");
        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(10.0);
        assertThat(trend.getGrowthRate().getObservationDays()).isEqualTo(5);
        assertThat(trend.getGrowthRate().getSpanDays()).isEqualTo(4);
        assertThat(trend.getSizeChange().isReady()).isTrue();
        assertThat(trend.getReference().isReady()).isFalse();
        assertThat(health(trend, 1)).isEqualTo("pending");
    }

    @Test
    void 이_포트가_며칠_전보다_얼마나_커졌는지_크기로_알려준다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(9), 0.03),                  // 7일보다 오래된 날과는 견주지 않는다
                photo(1, BASE.plusDays(10), 0.05), photo(1, BASE.plusDays(14), 0.084),
                photo(1, BASE.plusDays(15), 0.096), photo(1, BASE.plusDays(17), 0.114));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getSizeChange().isReady()).isTrue();
        assertThat(trend.getSizeChange().getDays()).isEqualTo(7);
        assertThat(trend.getSizeChange().getFromRatio()).isEqualTo(0.05);
        assertThat(trend.getSizeChange().getToRatio()).isEqualTo(0.114);
        assertThat(trend.getSizeChange().getChangePercent()).isEqualTo(128.0);
    }

    @Test
    void 관측이_하루뿐이면_과거와_견줄_수_없다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(4), 0.10), photo(1, BASE.plusDays(4).withHour(15), 0.11));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getSizeChange().isReady()).isFalse();
        assertThat(trend.getSizeChange().getChangePercent()).isNull();
    }

    @Test
    void 관측일이_부족하면_성장률을_내지_않는다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(3), 0.10), photo(1, BASE.plusDays(4), 0.12));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("pending");
        assertThat(trend.getGrowthRate().getRatePerDay()).isNull();
    }

    @Test
    void 잎만_잡힌_사진_한_장은_성장률을_흔들지_않는다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(1), 0.10), photo(1, BASE.plusDays(2), 0.11),
                photo(1, BASE.plusDays(3), 0.121), photo(1, BASE.plusDays(4), 0.01),
                photo(1, BASE.plusDays(5), 0.14641));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(10.0);
        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("growing");
    }

    @Test
    void 자라면서_성장률이_낮아지는_것은_둔화로_부르지_않는다() {
        givenPlants(port(1, BASE));
        List<Photo> photos = new ArrayList<>();
        double value = 0.01;
        for (int day = 1; day <= 14; day++) {
            photos.add(photo(1, BASE.plusDays(day), value));
            value *= day < 7 ? 1.20 : 1.05;
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getGrowthRate().getPreviousRatePerDay()).isEqualTo(20.0);
        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(5.0);
        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("growing");
    }

    @Test
    void 관측_크기가_줄면_감소로_표시한다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(3), 0.20), photo(1, BASE.plusDays(4), 0.18),
                photo(1, BASE.plusDays(5), 0.162));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(-10.0);
        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("down");
    }

    @Test
    void 수확이_끝난_재배와_나중에_심은_포트도_같은_시기_속도의_기준이_된다() {
        givenPlants(port(0, BASE.plusDays(30)), port(3, BASE.plusDays(25)));
        givenEndedPlants(endedPort(1, BASE, BASE.plusDays(20)),
                endedPort(2, BASE.plusDays(2), BASE.plusDays(22)));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 7; day++) {
            photos.add(photo(0, BASE.plusDays(30 + day), 0.10 * Math.pow(1.05, day)));   // 이 포트: 하루 5%
            photos.add(photo(1, BASE.plusDays(day), 0.03 * Math.pow(1.15, day)));        // 수확한 재배: 15%
            photos.add(photo(2, BASE.plusDays(2 + day), 0.20 * Math.pow(1.16, day)));    // 수확한 재배: 16%
            photos.add(photo(3, BASE.plusDays(25 + day), 0.02 * Math.pow(1.14, day)));   // 나중에 심은 포트: 14%
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 0);

        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(5.0);
        assertThat(trend.getReference().isReady()).isTrue();
        assertThat(trend.getReference().getPlantCount()).isEqualTo(3);
        assertThat(trend.getReference().getRatePerDay()).isEqualTo(15.0);
        assertThat(trend.getReference().getLowRatePerDay()).isEqualTo(14.5);
        assertThat(trend.getReference().getHighRatePerDay()).isEqualTo(15.5);
        assertThat(trend.getReference().getVerdict()).isEqualTo("slower");
        assertThat(health(trend, 0)).isEqualTo("warn");
        assertThat(health(trend, 3)).isEqualTo("good");
    }

    @Test
    void 과거_7번_한포트도_늦게_심은_포트의_같은_일차_참고_기준이_된다() {
        givenPlants(port(6, BASE), port(1, BASE.plusDays(20)));
        List<Photo> photos = new ArrayList<>();
        for (int day = 6; day <= 8; day++) {
            photos.add(photo(6, BASE.plusDays(day), .05 * Math.pow(1.15, day - 6)));
            photos.add(photo(1, BASE.plusDays(20 + day), .02 * Math.pow(1.05, day - 6)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getReference().isReady()).isTrue();
        assertThat(trend.getReference().getPlantCount()).isEqualTo(1);
        assertThat(trend.getReference().getPortIndices()).containsExactly(6);
        assertThat(trend.getReference().getVerdict()).isEqualTo("slower");
        assertThat(health(trend, 1)).isEqualTo("warn"); // 속도 문제가 보이면 다른 지표가 대기여도 주의다.
    }

    @Test
    void 한_비교_포트로도_크기와_속도가_모두_정상이면_초록이다() {
        givenPlants(port(0, BASE), port(1, BASE.plusDays(10)));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 5; day++) {
            photos.add(photo(0, BASE.plusDays(day), .05 * Math.pow(1.10, day - 3)));
            photos.add(photo(1, BASE.plusDays(10 + day), .08 * Math.pow(1.10, day - 3)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getReference().getPlantCount()).isEqualTo(1);
        assertThat(trend.getReference().getVerdict()).isEqualTo("similar");
        assertThat(health(trend, 0)).isEqualTo("good");
        assertThat(health(trend, 1)).isEqualTo("good");
    }

    @Test
    void 속도가_비슷해도_자신의_크기가_뚜렷이_줄면_주황이다() {
        givenPlants(port(0, BASE), port(1, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 5; day++) {
            photos.add(photo(0, BASE.plusDays(day), .08 * Math.pow(.90, day - 3)));
            photos.add(photo(1, BASE.plusDays(day), .12 * Math.pow(.90, day - 3)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getReference().getVerdict()).isEqualTo("similar");
        assertThat(trend.getSizeChange().getChangePercent()).isLessThan(-3.0);
        assertThat(health(trend, 1)).isEqualTo("warn");
    }

    @Test
    void 크기_감소만_확인돼도_속도_비교가_대기면_주황이다() {
        givenPlants(port(1, BASE));
        givenPhotos(photo(1, BASE.plusDays(3), .10), photo(1, BASE.plusDays(4), .08));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getSizeChange().isReady()).isTrue();
        assertThat(trend.getReference().isReady()).isFalse();
        assertThat(health(trend, 1)).isEqualTo("warn");
    }

    @Test
    void 같은_포트의_이전_재배는_다른_포트_속도_비교에_넣지_않는다() {
        givenPlants(port(1, BASE.plusDays(20)));
        givenEndedPlants(endedPort(1, BASE, BASE.plusDays(15)));
        List<Photo> photos = new ArrayList<>();
        for (int day = 6; day <= 8; day++) {
            photos.add(photo(1, BASE.plusDays(day), .05 * Math.pow(1.15, day - 6)));
            photos.add(photo(1, BASE.plusDays(20 + day), .02 * Math.pow(1.05, day - 6)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("growing");
        assertThat(trend.getReference().isReady()).isFalse();
    }

    @Test
    void 크기가_훨씬_작아도_같은_시기_속도가_비슷하면_정상으로_본다() {
        givenPlants(port(0, BASE), port(1, BASE), port(2, BASE), port(3, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 6; day++) {
            photos.add(photo(0, BASE.plusDays(day), 0.01 * Math.pow(1.10, day)));   // 작게 찍히지만 하루 10%
            photos.add(photo(1, BASE.plusDays(day), 0.10 * Math.pow(1.09, day)));
            photos.add(photo(2, BASE.plusDays(day), 0.20 * Math.pow(1.10, day)));
            photos.add(photo(3, BASE.plusDays(day), 0.30 * Math.pow(1.12, day)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 0);

        assertThat(trend.getGapPercent()).isLessThan(-15.0);   // 크기로만 보면 크게 뒤처진 포트
        assertThat(trend.getReference().getRatePerDay()).isEqualTo(10.0);
        assertThat(trend.getReference().getVerdict()).isEqualTo("similar");
        assertThat(health(trend, 0)).isEqualTo("good");
    }

    @Test
    void 같은_시기의_다른_재배보다_뚜렷이_빠르면_빠름으로_본다() {
        givenPlants(port(0, BASE), port(1, BASE), port(2, BASE), port(3, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 6; day++) {
            photos.add(photo(0, BASE.plusDays(day), 0.05 * Math.pow(1.20, day)));
            photos.add(photo(1, BASE.plusDays(day), 0.10 * Math.pow(1.09, day)));
            photos.add(photo(2, BASE.plusDays(day), 0.20 * Math.pow(1.10, day)));
            photos.add(photo(3, BASE.plusDays(day), 0.30 * Math.pow(1.12, day)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 0);

        assertThat(trend.getReference().getVerdict()).isEqualTo("faster");
        assertThat(health(trend, 0)).isEqualTo("good");
    }

    @Test
    void 다른_품종의_재배_기록은_속도_비교에_넣지_않는다() {
        givenPlants(port(0, BASE, 1L), port(1, BASE, 2L), port(2, BASE, 2L), port(3, BASE, 2L));
        List<Photo> photos = new ArrayList<>();
        for (int day = 3; day <= 6; day++) {
            photos.add(photo(0, BASE.plusDays(day), 0.05 * Math.pow(1.02, day)));
            for (int port = 1; port <= 3; port++) photos.add(photo(port, BASE.plusDays(day), 0.10 * Math.pow(1.20, day)));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 0);

        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(2.0);
        assertThat(trend.getReference().isReady()).isFalse();
        assertThat(health(trend, 0)).isEqualTo("pending");
    }

    @Test
    void 같은_포트의_이전_재배_사진은_새로_심은_식물에_섞이지_않는다() {
        LocalDateTime replantedAt = BASE.plusDays(10).withHour(13);
        givenPlants(port(1, replantedAt));
        givenEndedPlants(endedPort(1, BASE, BASE.plusDays(10).withHour(12)));
        givenPhotos(photo(1, BASE.plusDays(10).withHour(5), 0.90),   // 수확 전에 찍힌 이전 식물
                photo(1, BASE.plusDays(12).withHour(13), 0.02));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getPoints().get(0).getElapsedDay()).isEqualTo(2L);
        assertThat(trend.getLatestRatio()).isEqualTo(0.02);
    }

    @Test
    void 사진이_한_장뿐인_날이_있어도_찍힌_시각으로_비교해_성장률이_치우치지_않는다() {
        givenPlants(port(1, BASE));
        List<Photo> photos = new ArrayList<>();
        // 하루 10%씩 고르게 자라는 식물을 6시간마다 찍었다. 마지막 날은 새벽 한 장만 들어왔다.
        LocalDateTime first = BASE.plusDays(3).withHour(5);
        for (int shot = 0; shot <= 16; shot++) {
            photos.add(photoAt(1, first.plusHours(6L * shot), 0.05 * Math.pow(1.10, shot / 4.0), 0.30, 0.60));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(5);
        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(10.0);
        assertThat(trend.getGrowthRate().getObservationDays()).isEqualTo(5);
    }

    @Test
    void 포트를_옮겨_값이_한번에_달라져도_성장률은_옮긴_뒤_관측으로_낸다() {
        givenPlants(port(1, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 1; day <= 4; day++) {
            photos.add(photoAt(1, BASE.plusDays(day), 0.16 * Math.pow(1.10, day - 1), 0.30, 0.60));
        }
        // 5일차에 포트를 옮겨 식물이 작게 찍히기 시작했다. 자라는 속도는 그대로다.
        for (int day = 5; day <= 8; day++) {
            photos.add(photoAt(1, BASE.plusDays(day), 0.08 * Math.pow(1.10, day - 5), 0.60, 0.40));
        }
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getPoints()).hasSize(8);
        assertThat(trend.getViewChangedDay()).isEqualTo(5L);
        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("growing");
        assertThat(trend.getGrowthRate().getRatePerDay()).isEqualTo(10.0);
        assertThat(trend.getGrowthRate().getObservationDays()).isEqualTo(4);
    }

    @Test
    void 옮긴_직후에는_관측이_쌓일_때까지_성장률을_내지_않는다() {
        givenPlants(port(1, BASE));
        List<Photo> photos = new ArrayList<>();
        for (int day = 1; day <= 5; day++) photos.add(photoAt(1, BASE.plusDays(day), 0.16, 0.30, 0.60));
        photos.add(photoAt(1, BASE.plusDays(6), 0.08, 0.60, 0.40));
        photos.add(photoAt(1, BASE.plusDays(7), 0.09, 0.60, 0.40));
        givenPhotos(photos.toArray(Photo[]::new));

        GrowthTrendResponseDto trend = photoService.getGrowthTrend(SERIAL, 1);

        assertThat(trend.getViewChangedDay()).isEqualTo(6L);
        assertThat(trend.getGrowthRate().getStatus()).isEqualTo("pending");
        // 크기도 옮기기 전의 날과는 견주지 않는다: 옮긴 뒤 6일차 0.08 -> 7일차 0.09
        assertThat(trend.getSizeChange().getDays()).isEqualTo(1);
        assertThat(trend.getSizeChange().getChangePercent()).isEqualTo(12.5);
    }

    @Test
    void edgePhotosUseCaptureTimeAndNeverAttachUnknownCultivationToCurrentPlant() {
        Plant current = port(0, BASE); current.setId(22L);
        givenPlants(current);
        Photo delayed = photo(0, BASE.plusDays(8), 0.2);
        delayed.bindCapture(22L, BASE.plusDays(2).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant(), "delayed");
        Photo previous = photo(0, BASE.plusDays(9), 0.9);
        previous.bindCapture(11L, BASE.plusDays(3).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant(), "previous");
        givenPhotos(delayed, previous);
        var trend = photoService.getGrowthTrend(SERIAL, 0);
        assertThat(trend.getPoints()).hasSize(1);
        assertThat(trend.getPoints().get(0).getElapsedDay()).isEqualTo(2L);
        assertThat(trend.getPoints().get(0).getDate()).isEqualTo(BASE.plusDays(2).toLocalDate().toString());
    }

    private String health(GrowthTrendResponseDto trend, int portIndex) {
        return trend.getPorts().stream().filter(port -> port.getPortIndex() == portIndex)
                .findFirst().orElseThrow().getHealth();
    }

    private void givenPlants(Plant... plants) {
        when(plantRepository.findByDeviceIdAndEndedAtIsNull(SERIAL)).thenReturn(List.of(plants));
    }

    private void givenEndedPlants(Plant... plants) {
        when(plantRepository.findByDeviceIdAndEndedAtIsNotNull(SERIAL)).thenReturn(List.of(plants));
    }

    private Plant endedPort(int portIndex, LocalDateTime plantedAt, LocalDateTime endedAt) {
        Plant plant = port(portIndex, plantedAt);
        plant.setEndedAt(endedAt);
        return plant;
    }

    private void givenPhotos(Photo... photos) {
        when(photoRepository.findByDeviceAndBoxAreaRatioIsNotNullOrderByCreatedAtAsc(any(Device.class)))
                .thenReturn(new ArrayList<>(List.of(photos)));
    }

    private Plant port(int portIndex, LocalDateTime plantedAt) {
        return port(portIndex, plantedAt, 1L);
    }

    private Plant port(int portIndex, LocalDateTime plantedAt, Long speciesId) {
        Plant plant = new Plant();
        plant.setPortIndex(portIndex);
        plant.setPlantedAt(plantedAt);
        Species species = new Species();
        species.setId(speciesId);
        plant.setSpecies(species);
        return plant;
    }

    private Photo photo(int portIndex, LocalDateTime shotAt, double ratio) {
        return photo(portIndex, shotAt, ratio, 0.9, "1");
    }

    // 화면의 (centerX, centerY)에 넓이 ratio인 식물 박스 하나가 잡힌 사진.
    private Photo photoAt(int portIndex, LocalDateTime shotAt, double ratio, double centerX, double centerY) {
        double side = Math.sqrt(ratio);
        return photoWithDetections(portIndex, shotAt, ratio, String.format(java.util.Locale.ROOT,
                "[{\"cls\":\"Planting\",\"conf\":0.9,\"x\":%f,\"y\":%f,\"w\":%f,\"h\":%f}]",
                centerX, centerY, side, side));
    }

    private Photo photoWithDetections(int portIndex, LocalDateTime shotAt, double largestBoxRatio,
                                      String detectionsJson) {
        return photoWithDetections(portIndex, shotAt, largestBoxRatio, .9, detectionsJson);
    }

    private Photo photoWithDetections(int portIndex, LocalDateTime shotAt, double largestBoxRatio,
                                      double confidence, String detectionsJson) {
        Photo photo = spy(new Photo(device, portIndex, "/p.jpg", "p.jpg",
                "1", confidence, "no_detection", 0.0,
                10.0, 10.0, 100.0, largestBoxRatio, detectionsJson, 0));
        doReturn(shotAt).when(photo).getCreatedAt();
        return photo;
    }

    private Photo photoWithGreen(int portIndex, LocalDateTime shotAt, double boxRatio, double greenRatio) {
        Photo photo = spy(new Photo(device, portIndex, "/p.jpg", "p.jpg",
                "1", 0.9, "no_detection", 0.0,
                10.0, 10.0, 100.0, boxRatio, null, 0, greenRatio));
        doReturn(shotAt).when(photo).getCreatedAt();
        return photo;
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
