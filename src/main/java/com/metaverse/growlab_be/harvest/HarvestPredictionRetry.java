package com.metaverse.growlab_be.harvest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Retry date computation from saved inputs, without uploading the photo again. */
@Service @RequiredArgsConstructor @Slf4j
public class HarvestPredictionRetry {
    private final HarvestPredictionRepository predictions;
    private final DeviceRepository devices;
    private final HarvestInferenceClient inference;
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();

    @Scheduled(fixedDelayString="${harvest.retry-delay-ms:60000}",initialDelayString="${harvest.retry-initial-delay-ms:15000}")
    @Transactional
    public void retryPending() {
        for (HarvestPrediction row : predictions.findPendingActive(PageRequest.of(0,16))) {
            var device=devices.findById(row.getDeviceSerial()).orElse(null);
            int port=row.getPlant().getPortIndex();
            String bits=device==null?null:device.getPortStatus();
            if (!row.getPlant().isActive() || bits==null || port<0 || bits.length()<=port || bits.charAt(port)!='1') continue;
            try {
                List<Double> features=JSON.readValue(row.getFeaturesJson(),new TypeReference<List<Double>>() {});
                var input=new HarvestPayload(1,row.getObservationId(),row.getCapturedAt(),row.getPredictedAt(),
                        row.getModelVersion(),row.getFeatureVersion(),"FEATURES_READY",row.getAnchor(),row.getAnchorAt(),
                        null,null,features,2);
                row.completePrediction(inference.predict(input));
            } catch (Exception ex) {
                log.warn("EC2 prediction retry pending for saved observation {}",row.getObservationId());
            }
        }
    }
}
