package com.metaverse.growlab_be.photo.service;


import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.GrowthTrendResponseDto;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.dto.PhotoResponseDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository  photoRepository;
    private final DeviceRepository deviceRepository;
    private final PlantRepository  plantRepository;

    // 단계 반영과 같은 신뢰도 기준. 낮은 신뢰도의 박스는 크기 비교에 쓰지 않는다.
    private static final double MIN_GROWTH_CONFIDENCE = 0.70;

    @Value("${file.upload-dir.camera}")
    private String uploadDir;

    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto dto) throws IOException {
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(dto.getSerialNumber())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + dto.getSerialNumber()));

        if (dto.getPortIndex() == null) {
            throw new IllegalArgumentException("포트 번호(portIndex)가 누락되었습니다.");
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String fileName  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String filePath  = Paths.get(uploadDir, fileName).toString();
        imageFile.transferTo(new File(filePath));

        String  growthResult  = dto.getGrowthResult()      != null ? dto.getGrowthResult()      : "no_detection";
        Double  growthConf    = dto.getGrowthConfidence()  != null ? dto.getGrowthConfidence()  : 0.0;
        String  diseaseResult = dto.getDiseaseResult()     != null ? dto.getDiseaseResult()     : "no_detection";
        Double  diseaseConf   = dto.getDiseaseConfidence() != null ? dto.getDiseaseConfidence() : 0.0;

        Photo savedPhoto = photoRepository.save(new Photo(
                device, dto.getPortIndex(), filePath, fileName,
                growthResult, growthConf,
                diseaseResult, diseaseConf,
                dto.getBoxWidth(), dto.getBoxHeight(), dto.getBoxArea(), dto.getBoxAreaRatio(),
                dto.getDetectionsJson(), dto.getSelectedBoxIndex()
        ));

        return new PhotoResponseDto(savedPhoto);
    }

    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    /**
     * 포트별 성장 추이(관측 크기) + 또래 비교.
     * 신뢰도 70% 이상인 boxAreaRatio(프레임 대비 비율)만 관측값으로 사용한다.
     *
     * 비교축은 달력 날짜가 아니라 "파종 후 경과일"이다. 포트마다 파종 시점이 다르므로 같은
     * 날짜끼리 비교하면 늦게 심은 포트가 언제나 뒤처진 것으로 나온다(순차 파종을 하면 항상 틀린다).
     * 또래는 같은 품종의 Plant가 등록된 포트만 쓰며, 같은 경과일에 촬영된 값끼리 비교한다.
     * 미검출·낮은 신뢰도·오래된 값은 끌어오지 않는다.
     */
    @Transactional(readOnly = true)
    public GrowthTrendResponseDto getGrowthTrend(String serialNumber, Integer portIndex) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + serialNumber));

        // 활성 포트 = Plant가 등록된 포트. 파종 시각이 경과일 계산의 기준이 된다.
        Map<Integer, LocalDate> plantedDateByPort = new HashMap<>();
        Map<Integer, Plant> plantByPort = new HashMap<>();
        for (Plant plant : plantRepository.findByDeviceId(serialNumber)) {
            if (plant.getPortIndex() == null || plant.getPlantedAt() == null) continue;
            plantedDateByPort.put(plant.getPortIndex(), plant.getPlantedAt().toLocalDate());
            plantByPort.put(plant.getPortIndex(), plant);
        }

        List<Photo> photos = photoRepository.findByDeviceAndBoxAreaRatioIsNotNullOrderByCreatedAtAsc(device);

        // 포트 -> (경과일 -> [합, 개수]). 빈 포트와 파종 전 사진은 제외한다.
        Map<Integer, TreeMap<Long, double[]>> byPort = new HashMap<>();
        Map<Long, LocalDate> selfDateByElapsed = new HashMap<>();   // 화면 라벨용: 경과일 -> 그날 날짜
        for (Photo p : photos) {
            if (p.getBoxAreaRatio() == null || p.getPortIndex() == null || p.getCreatedAt() == null
                    || p.getConfidence() == null || p.getConfidence() < MIN_GROWTH_CONFIDENCE
                    || !Double.isFinite(p.getBoxAreaRatio()) || p.getBoxAreaRatio() <= 0
                    || p.getBoxAreaRatio() > 1
                    || p.getGrowthStageResult() == null
                    || "no_detection".equalsIgnoreCase(p.getGrowthStageResult())) continue;
            LocalDate planted = plantedDateByPort.get(p.getPortIndex());
            if (planted == null) continue;                          // 식물이 없는(수확해 비운) 포트
            LocalDate shot = p.getCreatedAt().toLocalDate();
            long elapsed = ChronoUnit.DAYS.between(planted, shot);
            if (elapsed < 0) continue;                              // 파종 전 사진
            double[] acc = byPort.computeIfAbsent(p.getPortIndex(), k -> new TreeMap<>())
                    .computeIfAbsent(elapsed, k -> new double[2]);
            acc[0] += p.getBoxAreaRatio();
            acc[1] += 1;
            if (p.getPortIndex().equals(portIndex)) selfDateByElapsed.putIfAbsent(elapsed, shot);
        }

        // 포트 -> (경과일 -> 평균)  ※ 조기 반환 전에 계산: 포트별 상태(점 색) 산출에 필요
        Map<Integer, TreeMap<Long, Double>> dailyAvg = new HashMap<>();
        for (Map.Entry<Integer, TreeMap<Long, double[]>> e : byPort.entrySet()) {
            TreeMap<Long, Double> m = new TreeMap<>();
            for (Map.Entry<Long, double[]> de : e.getValue().entrySet()) {
                m.put(de.getKey(), de.getValue()[0] / de.getValue()[1]);
            }
            dailyAvg.put(e.getKey(), m);
        }

        // 비교 근거가 없으면 포트 상태를 정상으로 추정하지 않는다.
        List<GrowthTrendResponseDto.PortHealth> portsHealth = computePortsHealth(plantByPort, dailyAvg);

        // 데이터 없음(로컬/초기) → 빈 시계열로 반환. 프론트가 "데이터 없음" 처리. (포트 상태는 채워 보낸다)
        if (!byPort.containsKey(portIndex)) {
            return new GrowthTrendResponseDto(portIndex, List.of(), List.of(),
                    null, null, null, null, portsHealth);
        }

        TreeMap<Long, Double> selfDaily = dailyAvg.get(portIndex);
        GrowthComparison comparison = comparePort(plantByPort, dailyAvg, portIndex);

        // 비교가 성립하기 전에는 본인 관측만 표시한다.
        List<GrowthTrendResponseDto.Point> points = new ArrayList<>();
        List<GrowthTrendResponseDto.Point> peerPoints = new ArrayList<>();
        for (Map.Entry<Long, Double> e : selfDaily.entrySet()) {
            long elapsed = e.getKey();
            LocalDate label = selfDateByElapsed.get(elapsed);
            String date = label != null ? label.toString() : null;
            points.add(new GrowthTrendResponseDto.Point(date, elapsed, round4(e.getValue())));
            if (comparison.ready()) {
                Double peerAvg = peerAverageAt(dailyAvg, comparison.peerPorts(), elapsed);
                peerPoints.add(new GrowthTrendResponseDto.Point(date, elapsed,
                        peerAvg != null ? round4(peerAvg) : null));
            }
        }

        return new GrowthTrendResponseDto(portIndex, points, peerPoints, comparison.rank(),
                comparison.portCount(), comparison.gapPercent(), round4(selfDaily.lastEntry().getValue()), portsHealth);
    }

    // 각 포트의 상태: 질병 > 충분한 비교에 따른 주의/정상 > 관측 대기.
    private static final double STALL_GAP_THRESHOLD = -15.0;
    private List<GrowthTrendResponseDto.PortHealth> computePortsHealth(
            Map<Integer, Plant> plantByPort, Map<Integer, TreeMap<Long, Double>> dailyAvg) {
        List<GrowthTrendResponseDto.PortHealth> out = new ArrayList<>();
        for (Map.Entry<Integer, Plant> e : plantByPort.entrySet()) {
            int port = e.getKey();
            String health = "pending";
            if (isDiseasedLabel(e.getValue().getDiseaseResult())) {
                health = "bad";
            } else {
                GrowthComparison comparison = comparePort(plantByPort, dailyAvg, port);
                if (comparison.ready())
                    health = comparison.gapPercent() <= STALL_GAP_THRESHOLD ? "warn" : "good";
            }
            out.add(new GrowthTrendResponseDto.PortHealth(port, health));
        }
        return out;
    }

    private record GrowthComparison(Integer rank, Integer portCount, Double gapPercent,
                                    List<Integer> peerPorts) {
        boolean ready() { return gapPercent != null; }
    }

    private GrowthComparison comparePort(Map<Integer, Plant> plantByPort,
                                         Map<Integer, TreeMap<Long, Double>> dailyAvg, int portIndex) {
        TreeMap<Long, Double> selfDaily = dailyAvg.get(portIndex);
        Plant self = plantByPort.get(portIndex);
        if (selfDaily == null || selfDaily.isEmpty() || self == null || self.getSpecies() == null)
            return new GrowthComparison(null, null, null, List.of());

        long latestDay = selfDaily.lastKey();
        double selfLatest = selfDaily.get(latestDay);
        List<Integer> peers = new ArrayList<>();
        double peerSum = 0;
        int higher = 0;
        for (Map.Entry<Integer, TreeMap<Long, Double>> entry : dailyAvg.entrySet()) {
            if (entry.getKey() == portIndex) continue;
            Plant peer = plantByPort.get(entry.getKey());
            if (peer == null || peer.getSpecies() == null
                    || !self.getSpecies().getId().equals(peer.getSpecies().getId())) continue;
            Double peerValue = entry.getValue().get(latestDay);
            if (peerValue == null) continue;
            peers.add(entry.getKey());
            peerSum += peerValue;
            if (peerValue > selfLatest) higher++;
        }
        if (peers.isEmpty() || peerSum <= 0)
            return new GrowthComparison(null, null, null, List.of());

        double peerAverage = peerSum / peers.size();
        return new GrowthComparison(higher + 1, peers.size() + 1,
                round1((selfLatest - peerAverage) / peerAverage * 100), peers);
    }

    // 질병 라벨 판정 (healthy/normal/정상/no_detection/빈값은 정상). FE HEALTHY_LABELS와 동일 기준.
    private boolean isDiseasedLabel(String label) {
        if (label == null) return false;
        String v = label.trim().toLowerCase();
        return !(v.isEmpty() || v.equals("no_detection") || v.equals("healthy") || v.equals("normal") || v.equals("정상"));
    }

    // 같은 경과일에 실제로 관측한 또래가 있을 때만 평균을 표시한다.
    private Double peerAverageAt(Map<Integer, TreeMap<Long, Double>> dailyAvg,
                                 List<Integer> peerPorts, long elapsed) {
        double sum = 0;
        int cnt = 0;
        for (Integer pp : peerPorts) {
            Double v = dailyAvg.get(pp).get(elapsed);
            if (v != null) { sum += v; cnt++; }
        }
        return cnt > 0 ? sum / cnt : null;
    }

    private Double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
    private Double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) return "";

        return fileName.substring(dotIndex + 1);
    }
}
