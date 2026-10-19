package com.metaverse.growlab_be.harvest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class HarvestService {
    public static final String MODEL="lettuce-ridge275-20261018-v1";
    public static final String FEATURES="selected-roi-rgb275-v1";
    private final HarvestPredictionRepository repository;
    private final PlantRepository plants;
    private final DeviceRepository devices;
    private final HarvestInferenceClient inferenceClient;
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();

    public Plant validateCultivation(Device device, Integer port, Long plantId, Instant capturedAt) {
        if (port==null || port<0 || port>7 || plantId==null || capturedAt==null) bad("촬영 식별정보가 필요합니다.");
        // Serialize photo ingestion with ending this cultivation. Delayed photos never attach to a new plant.
        Plant plant=plants.findLockedById(plantId).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"종료되었거나 없는 재배입니다."));
        String bits=device.getPortStatus();
        if (!plant.isActive() || !device.getId().equals(plant.getDevice().getId()) || !port.equals(plant.getPortIndex())
                || bits==null || bits.length()<=port || bits.charAt(port)!='1') {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"촬영 당시 재배와 현재 활성 포트가 다릅니다.");
        }
        if (capturedAt.isAfter(Instant.now().plusSeconds(300))) bad("미래 촬영 시각입니다.");
        // plantedAt is a user-entered date, not a reliable capture-time boundary. plantId is the lifecycle key.
        return plant;
    }

    public HarvestPayload parseAndValidate(String json, String observationId, Instant capturedAt, Plant plant) {
        HarvestPayload p;
        try { UUID.fromString(observationId); p=JSON.readValue(json,HarvestPayload.class); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"잘못된 수확기 예측 형식입니다."); }
        if (p==null || !Integer.valueOf(1).equals(p.schemaVersion()) || !observationId.equals(p.observationId())
                || !capturedAt.equals(p.capturedAt()) || !capturedAt.equals(p.predictedAt())
                || !MODEL.equals(p.modelVersion()) || !FEATURES.equals(p.featureVersion())) bad("예측 버전 또는 촬영 시각 불일치입니다.");
        Set<String> statuses=Set.of("FEATURES_READY","PREDICTED","HARVEST_READY","WAITING_ANCHOR","NO_DETECTION","UNSUPPORTED_CROP");
        if (p.status()==null || !statuses.contains(p.status())) bad("알 수 없는 예측 상태입니다.");
        boolean supported=HarvestCrop.supports(plant.getSpecies());
        if (supported=="UNSUPPORTED_CROP".equals(p.status())) bad("품종과 예측 모델이 일치하지 않습니다.");
        if(p.confirmedStage()==null || p.confirmedStage()<0 || p.confirmedStage()>3
                || ("HARVEST_READY".equals(p.status()) != (p.confirmedStage()==3))
                || (!supported && p.confirmedStage()!=0)) bad("잘못된 누적 확인 단계입니다.");
        if (p.anchor()!=null && (!Set.of("growing","planting").contains(p.anchor()) || p.anchorAt()==null
                || p.anchorAt().isAfter(capturedAt))) bad("잘못된 관측 기준일입니다.");
        if (p.anchor()==null && p.anchorAt()!=null) bad("관측 기준 종류가 없습니다.");
        if ("FEATURES_READY".equals(p.status()) || "PREDICTED".equals(p.status())) {
            if (p.anchor()==null) bad("관측 기준일이 없습니다.");
            validateFeatures(p);
        }
        if ("FEATURES_READY".equals(p.status())) {
            if (p.remainingDays()!=null || p.expectedAt()!=null) bad("기기에서 예측 날짜를 보낼 수 없습니다.");
        } else if ("PREDICTED".equals(p.status())) {
            if (p.anchor()==null || p.remainingDays()==null || !Double.isFinite(p.remainingDays()) || p.remainingDays()<0
                    || p.expectedAt()==null) bad("잔여일 또는 예상일이 없습니다.");
            double seconds=Duration.between(p.predictedAt(),p.expectedAt()).toMillis()/1000.;
            if (Math.abs(seconds-p.remainingDays()*86400)>1) bad("잔여일과 예상일이 다릅니다.");
        } else if ("HARVEST_READY".equals(p.status())) {
            if (p.remainingDays()==null || p.remainingDays()!=0. || p.expectedAt()==null || p.expectedAt().isAfter(capturedAt)) bad("수확기 확인 정보가 잘못되었습니다.");
            if(p.features()!=null) bad("확인 상태에는 예측 입력을 넣지 않습니다.");
        } else if(p.expectedAt()!=null || p.remainingDays()!=null || p.features()!=null) bad("예측 전 상태에 예상일을 넣을 수 없습니다.");
        return p;
    }

    private void validateFeatures(HarvestPayload p) {
        List<Double> f=p.features();
        if (f==null || f.size()!=275 || f.stream().anyMatch(v->v==null || !Double.isFinite(v))
                || f.get(274)!=0. || f.get(0)<0 || f.get(8)<0 || f.get(8)>6
                || Math.abs(f.get(0)-Duration.between(p.anchorAt(),p.predictedAt()).toMillis()/86400000.)>1e-6)
            bad("잘못된 275차원 입력입니다.");
    }

    public HarvestPayload predictOnEc2(HarvestPayload input) {
        return "FEATURES_READY".equals(input.status()) || "PREDICTED".equals(input.status())
                ? inferenceClient.predict(input) : input;
    }

    public void save(Plant plant, Long photoId, HarvestPayload p) {
        try { repository.save(new HarvestPrediction(plant,photoId,p,p.features()==null?null:JSON.writeValueAsString(p.features()))); }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalArgumentException(e); }
    }

    @Transactional(readOnly=true)
    public View latest(Long plantId, Long userId) {
        Plant plant=plants.findByIdAndUserId(plantId,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Refresh device state: don't expose an old forecast while this port is OFF.
        Device device=devices.findById(plant.getDevice().getId()).orElseThrow();
        int port=plant.getPortIndex();String bits=device.getPortStatus();
        if(bits==null || bits.length()<=port || bits.charAt(port)!='1') return View.empty(plantId,"PORT_OFF");
        if(!HarvestCrop.supports(plant.getSpecies())) return View.empty(plantId,"UNSUPPORTED_CROP");
        return repository.findTopByPlantIdOrderByCapturedAtDesc(plantId).map(p->new View(plantId,
                p.getStatus(),p.getCapturedAt(),p.getPredictedAt(),p.getExpectedAt(),p.getRemainingDays(),
                p.getAnchor(),p.getAnchorAt(),p.getModelVersion(),
                p.getCapturedAt().isBefore(Instant.now().minusSeconds(8*3600)),"YOLO_HARVEST_CONFIRMATION"))
                .orElseGet(()->View.empty(plantId,"WAITING_OBSERVATION"));
    }
    public record View(Long plantId,String status,Instant capturedAt,Instant predictedAt,Instant expectedAt,
                       Double remainingDays,String anchor,Instant anchorAt,String modelVersion,boolean stale,String target) {
        static View empty(Long id,String status) { return new View(id,status,null,null,null,null,null,null,null,false,"YOLO_HARVEST_CONFIRMATION"); }
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
