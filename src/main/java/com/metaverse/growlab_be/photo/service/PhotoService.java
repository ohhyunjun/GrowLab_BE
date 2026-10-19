package com.metaverse.growlab_be.photo.service;


import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.file.service.S3StorageService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository  photoRepository;
    private final DeviceRepository deviceRepository;
    private final PlantRepository  plantRepository;
    private final S3StorageService s3StorageService;
    private final com.metaverse.growlab_be.harvest.HarvestService harvestService;
    private final com.metaverse.growlab_be.plant.service.PlantService plantService;

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

        boolean edge=dto.getPlantId()!=null || dto.getObservationId()!=null || dto.getCapturedAt()!=null || dto.getHarvestPredictionJson()!=null;
        Device device = (edge ? deviceRepository.findLockedById(dto.getSerialNumber()) : deviceRepository.findById(dto.getSerialNumber()))
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + dto.getSerialNumber()));

        if (dto.getPortIndex() == null) {
            throw new IllegalArgumentException("포트 번호(portIndex)가 누락되었습니다.");
        }

        Plant capturePlant=null;
        java.time.Instant capturedAt=null;
        com.metaverse.growlab_be.harvest.HarvestPayload harvest=null;
        if (edge) {
            try { capturedAt=java.time.Instant.parse(dto.getCapturedAt()); }
            catch(Exception e) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"촬영 시각에 UTC 시간대가 필요합니다."); }
            capturePlant=harvestService.validateCultivation(device,dto.getPortIndex(),dto.getPlantId(),capturedAt);
            harvest=harvestService.parseAndValidate(dto.getHarvestPredictionJson(),dto.getObservationId(),capturedAt,capturePlant);
            var previous=photoRepository.findByDeviceIdAndObservationId(device.getId(),dto.getObservationId());
            if(previous.isPresent()) {
                Photo p=previous.get();
                if(!dto.getPlantId().equals(p.getPlantId()) || !capturedAt.equals(p.getCapturedAt()))
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"재사용된 관측 ID입니다.");
                return new PhotoResponseDto(p);
            }
            // The device supplies image features; EC2 returns the authoritative harvest date.
            harvest=harvestService.predictOnEc2(harvest);
        }

        String fileName;
        String filePath;
        if (s3StorageService.isEnabled()) {
            S3StorageService.UploadResult uploaded = s3StorageService.upload(imageFile, "uploads/camera");
            fileName = uploaded.storedFileName();
            filePath = uploaded.url();
        } else {
            File directory = new File(uploadDir);
            if (!directory.exists()) directory.mkdirs();
            String extension = getFileExtension(imageFile.getOriginalFilename());
            fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + "_" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            filePath = Paths.get(uploadDir, fileName).toString();
            imageFile.transferTo(new File(filePath));
        }

        String  growthResult  = dto.getGrowthResult()      != null ? dto.getGrowthResult()      : "no_detection";
        Double  growthConf    = dto.getGrowthConfidence()  != null ? dto.getGrowthConfidence()  : 0.0;
        String  diseaseResult = dto.getDiseaseResult()     != null ? dto.getDiseaseResult()     : "no_detection";
        Double  diseaseConf   = dto.getDiseaseConfidence() != null ? dto.getDiseaseConfidence() : 0.0;

        Photo photo = new Photo(
                device, dto.getPortIndex(), filePath, fileName,
                growthResult, growthConf,
                diseaseResult, diseaseConf,
                dto.getBoxWidth(), dto.getBoxHeight(), dto.getBoxArea(), dto.getBoxAreaRatio(),
                dto.getDetectionsJson(), dto.getSelectedBoxIndex()
        );
        if(edge) photo.bindCapture(dto.getPlantId(),capturedAt,dto.getObservationId());
        Photo savedPhoto=photoRepository.save(photo);
        if(edge) {
            harvestService.save(capturePlant,savedPhoto.getId(),harvest);
            // Growth confirmation for lettuce comes from the accumulated photo history, not a single frame.
            String confirmedGrowth=growthResult;
            Double confirmedConfidence=growthConf;
            if(com.metaverse.growlab_be.harvest.HarvestCrop.supports(capturePlant.getSpecies())) {
                confirmedGrowth=harvest.confirmedStage()>0?harvest.confirmedStage().toString():"no_detection";
                // Internal confirmation flag; never exposed as an AI probability/forecast confidence.
                confirmedConfidence=harvest.confirmedStage()>0?1.:0.;
            }
            if(!photoRepository.existsByPlantIdAndCapturedAtAfter(capturePlant.getId(),capturedAt))
                plantService.applyObservationForPlant(capturePlant,confirmedGrowth,confirmedConfidence,diseaseResult,diseaseConf);
        }

        return new PhotoResponseDto(savedPhoto);
    }

    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    /**
     * 포트별 성장 추이.
     *
     * 크기와 속도를 다르게 다룬다.
     *  - 크기  : 그 포트 자신의 시계열과 "며칠 전보다 몇 % 커졌는지"로 보여준다. 크기는 포트가 화면 어디에
     *            어떻게 찍히느냐에 좌우되므로 다른 포트의 크기와 견주지 않는다.
     *  - 속도  : "하루에 몇 %씩 자랐는지"는 찍히는 자리와 무관한 비율이라 포트끼리 견줄 수 있다.
     *            같은 기기에서 같은 품종을 키운 다른 재배 기록(재배 중 + 종료)이 "심은 뒤 같은 시기"에
     *            보인 속도와 비교한다. 심은 시기가 달라도, 이미 수확한 개체여도 비교 대상이 된다.
     *
     * 신뢰도 70% 이상인 사진만 쓰고, 크기는 검출 박스 목록에서 고른 식물 범위({@link PlantExtent})의
     * 프레임 대비 면적 비율이다. 식물이 화면 밖으로 잘린 사진은 크기를 알 수 없어 뺀다.
     * 그래프에는 그날 사진의 중앙값을 하루 값으로 쓰고, 성장 속도는 사진 하나하나를 찍힌 시각으로
     * 비교해 구한다. 포트를 옮겨 찍히는 자리가 바뀐 경우의 처리는 {@link PlantSeries} 참고.
     *
     * rank·portCount·gapPercent·peerPoints는 같은 경과일의 크기를 활성 포트끼리 견주던 예전 값이다.
     * 응답 형식을 유지하려고 남겨 두었고, 포트 상태 판정에는 쓰지 않는다.
     */
    @Transactional(readOnly = true)
    public GrowthTrendResponseDto getGrowthTrend(String serialNumber, Integer portIndex) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + serialNumber));

        // 활성 포트 = Plant가 등록된 포트. 파종 시각이 경과일 계산의 기준이 된다.
        Map<Integer, Plant> plantByPort = new HashMap<>();
        for (Plant plant : plantRepository.findByDeviceIdAndEndedAtIsNull(serialNumber)) {
            if (plant.getPortIndex() == null || plant.getPlantedAt() == null) continue;
            plantByPort.put(plant.getPortIndex(), plant);
        }
        // 종료된 재배 기록은 품종 기준에만 쓴다. 사진은 그 식물이 포트에 있던 기간의 것만 붙인다.
        Map<Integer, List<Plant>> endedByPort = new HashMap<>();
        for (Plant plant : plantRepository.findByDeviceIdAndEndedAtIsNotNull(serialNumber)) {
            if (plant.getPortIndex() == null || plant.getPlantedAt() == null || plant.getEndedAt() == null) continue;
            endedByPort.computeIfAbsent(plant.getPortIndex(), k -> new ArrayList<>()).add(plant);
        }

        List<Photo> photos = photoRepository.findByDeviceAndBoxAreaRatioIsNotNullOrderByCreatedAtAsc(device);
        Plant selfPlant = plantByPort.get(portIndex);

        // 식물 -> 관측 목록. 빈 포트와 파종 전 사진은 제외한다.
        Map<Plant, List<PlantSeries.Observation>> observed = new IdentityHashMap<>();
        Map<Long, LocalDate> selfDateByElapsed = new HashMap<>();   // 화면 라벨용: 경과일 -> 그날 날짜
        for (Photo p : photos) {
            if (p.getBoxAreaRatio() == null || p.getPortIndex() == null || p.getCreatedAt() == null
                    || p.getConfidence() == null || p.getConfidence() < MIN_GROWTH_CONFIDENCE
                    || !Double.isFinite(p.getBoxAreaRatio()) || p.getBoxAreaRatio() <= 0
                    || p.getBoxAreaRatio() > 1
                    || p.getGrowthStageResult() == null
                    || "no_detection".equalsIgnoreCase(p.getGrowthStageResult())) continue;
            // 검출 목록이 저장되지 않은 옛 사진만 저장된 최대 면적 박스를 그대로 쓴다.
            PlantExtent.Result extent = PlantExtent.of(p.getDetectionsJson());
            Plant owner = ownerOf(p, plantByPort, endedByPort);
            if (owner == null) continue;                            // 식물이 없던 때의 사진
            LocalDate shot = shotAt(p).toLocalDate();
            long elapsed = ChronoUnit.DAYS.between(owner.getPlantedAt().toLocalDate(), shot);
            if (elapsed < 0) continue;
            observed.computeIfAbsent(owner, k -> new ArrayList<>()).add(extent != null
                    ? new PlantSeries.Observation(shotAt(p), elapsed, extent.areaRatio(), extent.cut(),
                            extent.centerX(), extent.centerY())
                    : new PlantSeries.Observation(shotAt(p), elapsed, p.getBoxAreaRatio(), false, null, null));
            if (owner == selfPlant) selfDateByElapsed.putIfAbsent(elapsed, shot);
        }

        // 식물 -> 정리된 관측. 쓸 수 있는 날이 하나도 없는 식물은 넣지 않는다.
        Map<Plant, PlantSeries> seriesByPlant = new IdentityHashMap<>();
        for (Map.Entry<Plant, List<PlantSeries.Observation>> e : observed.entrySet()) {
            PlantSeries series = PlantSeries.of(e.getValue());
            if (!series.daily().isEmpty()) seriesByPlant.put(e.getKey(), series);
        }
        // 활성 포트 -> (경과일 -> 그날의 크기)
        Map<Integer, TreeMap<Long, Double>> dailyAvg = new HashMap<>();
        for (Map.Entry<Integer, Plant> e : plantByPort.entrySet()) {
            PlantSeries series = seriesByPlant.get(e.getValue());
            if (series != null) dailyAvg.put(e.getKey(), series.daily());
        }

        // 비교 근거가 없으면 포트 상태를 정상으로 추정하지 않는다.
        List<GrowthTrendResponseDto.PortHealth> portsHealth = computePortsHealth(plantByPort, seriesByPlant);

        // 데이터 없음(로컬/초기) → 빈 시계열로 반환. 프론트가 "데이터 없음" 처리. (포트 상태는 채워 보낸다)
        PlantSeries selfSeries = seriesByPlant.get(selfPlant);
        if (selfSeries == null) {
            return new GrowthTrendResponseDto(portIndex, List.of(), List.of(),
                    null, null, null, null, portsHealth,
                    GrowthTrendResponseDto.SizeChange.pending(), GrowthTrendResponseDto.GrowthRate.pending(),
                    GrowthTrendResponseDto.Reference.notReady(), null);
        }

        TreeMap<Long, Double> selfDaily = selfSeries.daily();
        GrowthComparison comparison = comparePort(plantByPort, dailyAvg, portIndex);

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

        RateReference rateReference = rateReferenceOf(selfPlant, seriesByPlant);
        GrowthTrendResponseDto.Reference reference = rateReference == null
                ? GrowthTrendResponseDto.Reference.notReady()
                : new GrowthTrendResponseDto.Reference(true, round1(rateReference.median()),
                        round1(rateReference.low()), round1(rateReference.high()),
                        rateReference.count(), rateReference.verdict());

        return new GrowthTrendResponseDto(portIndex, points, peerPoints, comparison.rank(),
                comparison.portCount(), comparison.gapPercent(), round4(selfDaily.lastEntry().getValue()), portsHealth,
                sizeChangeOf(selfSeries), growthRateOf(selfSeries), reference, selfSeries.lastViewChangeDay());
    }

    // 사진이 찍힌 시각에 그 포트에 있던 식물. 종료된 재배는 [파종, 종료) 구간으로, 재배 중인 식물은 파종일 이후로 판정한다.
    private static LocalDateTime shotAt(Photo photo) {
        return photo.getCapturedAt() == null ? photo.getCreatedAt()
                : photo.getCapturedAt().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }

    private Plant ownerOf(Photo photo, Map<Integer, Plant> plantByPort, Map<Integer, List<Plant>> endedByPort) {
        if (photo.getPlantId() != null) {
            Plant current = plantByPort.get(photo.getPortIndex());
            if (current != null && photo.getPlantId().equals(current.getId())) return current;
            for (Plant ended : endedByPort.getOrDefault(photo.getPortIndex(), List.of())) {
                if (photo.getPlantId().equals(ended.getId())) return ended;
            }
            return null;
        }

        LocalDateTime shotAt = shotAt(photo);
        for (Plant ended : endedByPort.getOrDefault(photo.getPortIndex(), List.of())) {
            if (!shotAt.isBefore(ended.getPlantedAt()) && shotAt.isBefore(ended.getEndedAt())) return ended;
        }
        Plant active = plantByPort.get(photo.getPortIndex());
        if (active == null || shotAt.toLocalDate().isBefore(active.getPlantedAt().toLocalDate())) return null;
        return active;
    }

    // ── 1) 이 포트의 과거 대비 크기 ──────────────────────────────
    // 최근 7일 안에서 가장 이른 날의 크기와 가장 최근 날의 크기를 견준다. 포트를 옮겼으면 옮긴 뒤의 날끼리만 견준다.
    private static final int SIZE_CHANGE_WINDOW_DAYS = 7;

    private GrowthTrendResponseDto.SizeChange sizeChangeOf(PlantSeries self) {
        long latestDay = self.daily().lastKey();
        NavigableMap<Long, Double> recent = self.latestViewDaily(latestDay - SIZE_CHANGE_WINDOW_DAYS, latestDay);
        if (recent.size() < 2) return GrowthTrendResponseDto.SizeChange.pending();
        double from = recent.firstEntry().getValue();
        double to = recent.lastEntry().getValue();
        return new GrowthTrendResponseDto.SizeChange(true, (int) (recent.lastKey() - recent.firstKey()),
                round4(from), round4(to), round1((to / from - 1) * 100));
    }

    // ── 2) 이 포트의 속도 (다른 재배와 견주는 데 쓴다) ────────────
    // 최근 7일 안의 사진으로 하루 성장률을 구한다. 관측일이 3일 미만이거나 이틀 이상 벌어지지 않으면 내지 않는다.
    // 포트를 옮겨 찍히는 자리가 바뀌었으면, 최근 속도는 지금 자리에서 본 관측만으로 계산한다.
    private static final int RATE_WINDOW_DAYS = 7;
    // 박스 면적은 사진마다 흔들리므로 ±1%/일 안쪽은 변화로 보지 않는다.
    // 하루 성장률(%)은 식물이 클수록 자연히 낮아지므로, 직전 구간보다 낮다는 것만으로는 상태를 바꾸지 않는다.
    private static final double RATE_FLAT_BAND = 1.0;

    private GrowthTrendResponseDto.GrowthRate growthRateOf(PlantSeries self) {
        long latestDay = self.daily().lastKey();
        List<PlantSeries.Observation> recent = self.latestViewPhotos(latestDay - RATE_WINDOW_DAYS + 1, latestDay);
        Double rate = GrowthMath.ratePerDayPercent(List.of(recent));
        if (rate == null) return GrowthTrendResponseDto.GrowthRate.pending();

        Double previous = GrowthMath.ratePerDayPercent(self.sameViewPhotos(
                latestDay - 2L * RATE_WINDOW_DAYS + 1, latestDay - RATE_WINDOW_DAYS));
        String status;
        if (rate <= -RATE_FLAT_BAND) status = "down";
        else if (rate < RATE_FLAT_BAND) status = "flat";
        else status = "growing";
        return new GrowthTrendResponseDto.GrowthRate(round1(rate), previous != null ? round1(previous) : null,
                status, (int) recent.stream().mapToLong(PlantSeries.Observation::elapsedDay).distinct().count(),
                (int) (recent.get(recent.size() - 1).elapsedDay() - recent.get(0).elapsedDay()));
    }

    // ── 3) 같은 시기의 속도 비교 ───────────────────────────────────
    // 같은 품종의 다른 재배 기록이 이 수 이상 있어야 견줄 기준으로 쓴다.
    private static final int MIN_REFERENCE_PLANTS = 3;
    // 하루 성장률 차이가 이 %p 안쪽이면 비슷한 것으로 본다. 관측일이 하루 이틀 달라지는 것만으로도 이 정도는 움직인다.
    private static final double RATE_SIMILAR_BAND = 3.0;

    /** verdict: faster · similar · slower. median/low/high는 다른 재배 기록들의 하루 성장률(%). */
    private record RateReference(double median, double low, double high, int count, String verdict) {}

    // 이 식물의 최근 속도를, 같은 품종의 다른 재배 기록이 심은 뒤 같은 시기(같은 경과일 구간)에 보인 속도와 견준다.
    private RateReference rateReferenceOf(Plant self, Map<Plant, PlantSeries> seriesByPlant) {
        PlantSeries own = self == null ? null : seriesByPlant.get(self);
        if (own == null || self.getSpecies() == null || self.getSpecies().getId() == null) return null;
        long latestDay = own.daily().lastKey();
        long fromDay = latestDay - RATE_WINDOW_DAYS + 1;
        Double ownRate = GrowthMath.ratePerDayPercent(List.of(own.latestViewPhotos(fromDay, latestDay)));
        if (ownRate == null) return null;

        List<Double> rates = new ArrayList<>();
        for (Map.Entry<Plant, PlantSeries> entry : seriesByPlant.entrySet()) {
            Plant other = entry.getKey();
            if (other == self || other.getSpecies() == null
                    || !self.getSpecies().getId().equals(other.getSpecies().getId())) continue;
            Double rate = GrowthMath.ratePerDayPercent(entry.getValue().sameViewPhotos(fromDay, latestDay));
            if (rate != null) rates.add(rate);
        }
        if (rates.size() < MIN_REFERENCE_PLANTS) return null;
        Collections.sort(rates);
        double median = GrowthMath.percentile(rates, 0.50);
        double low = GrowthMath.percentile(rates, 0.25);
        double high = GrowthMath.percentile(rates, 0.75);
        // 다른 기록들의 범위를 벗어나고 중앙값과도 뚜렷이 벌어졌을 때만 빠르다·느리다고 한다.
        String verdict = ownRate < low && ownRate <= median - RATE_SIMILAR_BAND ? "slower"
                : ownRate > high && ownRate >= median + RATE_SIMILAR_BAND ? "faster" : "similar";
        return new RateReference(median, low, high, rates.size(), verdict);
    }

    // 각 포트의 상태: 질병 > 같은 시기의 속도 비교 > 관측 대기. 크기는 포트끼리 견주지 않는다.
    private List<GrowthTrendResponseDto.PortHealth> computePortsHealth(
            Map<Integer, Plant> plantByPort, Map<Plant, PlantSeries> seriesByPlant) {
        List<GrowthTrendResponseDto.PortHealth> out = new ArrayList<>();
        for (Map.Entry<Integer, Plant> e : plantByPort.entrySet()) {
            String health = "pending";
            if (isDiseasedLabel(e.getValue().getDiseaseResult())) {
                health = "bad";
            } else {
                RateReference reference = rateReferenceOf(e.getValue(), seriesByPlant);
                if (reference != null) health = "slower".equals(reference.verdict()) ? "warn" : "good";
            }
            out.add(new GrowthTrendResponseDto.PortHealth(e.getKey(), health));
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
