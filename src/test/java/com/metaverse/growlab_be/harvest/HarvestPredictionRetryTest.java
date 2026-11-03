package com.metaverse.growlab_be.harvest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class HarvestPredictionRetryTest {
    @Test void savedInputsAreRetriedWithoutAnotherPhotoUpload() throws Exception { exercise(false,false); }
    @Test void outageKeepsInputsPendingForNextRetry() throws Exception { exercise(true,false); }
    @Test void offPortDoesNotTriggerInference() throws Exception { exercise(false,true); }

    void exercise(boolean outage,boolean off) throws Exception {
        var repository=mock(HarvestPredictionRepository.class);
        var devices=mock(DeviceRepository.class);
        var inference=mock(HarvestInferenceClient.class);
        var retry=new HarvestPredictionRetry(repository,devices,inference);
        Device device=new Device("TEST","test");device.setPortStatus(off?"00000000":"10000000");
        Plant plant=new Plant();plant.setDevice(device);plant.setPortIndex(0);
        Instant time=Instant.parse("2026-10-26T00:00:00Z");
        var features=new ArrayList<>(Collections.nCopies(275,0.));
        var input=new HarvestPayload(1,UUID.randomUUID().toString(),time,time,HarvestService.MODEL,
                HarvestService.FEATURES,"FEATURES_READY","growing",time,null,null,features,2);
        var row=new HarvestPrediction(plant,123L,input,new ObjectMapper().writeValueAsString(features));
        when(repository.findPendingActive(any())).thenReturn(List.of(row));
        when(devices.findById("TEST")).thenReturn(Optional.of(device));
        if(outage) when(inference.predict(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        else when(inference.predict(any())).thenAnswer(call->{
            HarvestPayload p=call.getArgument(0);
            return new HarvestPayload(p.schemaVersion(),p.observationId(),p.capturedAt(),p.predictedAt(),
                    p.modelVersion(),p.featureVersion(),"PREDICTED",p.anchor(),p.anchorAt(),2.,time.plusSeconds(172800),p.features(),2);
        });
        retry.retryPending();
        assertThat(row.getPhotoId()).isEqualTo(123L);
        if(off) verifyNoInteractions(inference);
        if(outage || off) { assertThat(row.getStatus()).isEqualTo("FEATURES_READY");assertThat(row.getExpectedAt()).isNull(); }
        else { assertThat(row.getStatus()).isEqualTo("PREDICTED");assertThat(row.getRemainingDays()).isEqualTo(2.); }
    }
}
