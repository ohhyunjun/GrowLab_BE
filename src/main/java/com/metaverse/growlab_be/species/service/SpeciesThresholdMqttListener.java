package com.metaverse.growlab_be.species.service;

import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.device.service.MqttPublisher;
import com.metaverse.growlab_be.species.domain.Species;
import com.metaverse.growlab_be.species.event.SpeciesThresholdsUpdatedEvent;
import com.metaverse.growlab_be.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeciesThresholdMqttListener {

    private final SpeciesRepository speciesRepository;
    private final DeviceRepository deviceRepository;
    private final ObjectProvider<MqttPublisher> mqttPublisherProvider;

    // 품종 수정이 DB에 확정된 뒤, 그 품종을 사용하는 모든 기기에 새 범위를 재발행한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void publishUpdatedThresholds(SpeciesThresholdsUpdatedEvent event) {
        MqttPublisher publisher = mqttPublisherProvider.getIfAvailable();
        if (publisher == null) return;

        Species species = speciesRepository.findById(event.speciesId()).orElse(null);
        if (species == null) {
            log.warn("[MQTT] 수정된 품종을 찾을 수 없어 thresholds 재발행 생략 - speciesId={}",
                    event.speciesId());
            return;
        }

        for (Device device : deviceRepository.findBySpecies_Id(event.speciesId())) {
            try {
                publisher.publishThresholds(device.getId(), species);
            } catch (RuntimeException e) {
                log.warn("[MQTT] 품종 수정 thresholds 재발행 실패 - speciesId={}, serial={}",
                        event.speciesId(), device.getId(), e);
            }
        }
    }
}
