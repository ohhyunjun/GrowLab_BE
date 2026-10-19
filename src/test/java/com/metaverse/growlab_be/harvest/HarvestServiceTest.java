package com.metaverse.growlab_be.harvest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.domain.PlantEndReason;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import com.metaverse.growlab_be.species.domain.Species;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HarvestServiceTest {
    HarvestPredictionRepository predictions=mock(HarvestPredictionRepository.class);
    PlantRepository plants=mock(PlantRepository.class);
    DeviceRepository devices=mock(DeviceRepository.class);
    HarvestInferenceClient inference=mock(HarvestInferenceClient.class);
    HarvestService service=new HarvestService(predictions,plants,devices,inference);
    ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    Device device;Plant plant;
    final Instant time=Instant.parse("2026-10-01T00:00:00Z");
    final String observation="2e8b598d-1234-4b89-9767-123456789abc";

    @BeforeEach void setup() {
        device=new Device("TEST","Test");device.setPortStatus("10000000");
        Species species=new Species();species.setName("청상추");
        plant=new Plant();plant.setId(1L);plant.setPortIndex(0);plant.setDevice(device);plant.setSpecies(species);
        when(plants.findLockedById(1L)).thenReturn(Optional.of(plant));
    }
    Map<String,Object> valid() {
        Map<String,Object> p=new HashMap<>();
        p.put("schemaVersion",1);p.put("observationId",observation);p.put("capturedAt",time);p.put("predictedAt",time);
        p.put("modelVersion",HarvestService.MODEL);p.put("featureVersion",HarvestService.FEATURES);
        p.put("status","PREDICTED");p.put("confirmedStage",2);p.put("anchor","growing");p.put("anchorAt",time);
        p.put("remainingDays",2.5);p.put("expectedAt",time.plusSeconds(216000));
        p.put("features",new ArrayList<>(Collections.nCopies(275,0.)));return p;
    }
    HarvestPayload parse(Map<String,Object> p) throws Exception {
        return service.parseAndValidate(json.writeValueAsString(p),observation,time,plant);
    }
    @Test void acceptsVersioned275Input() throws Exception {
        assertThat(parse(valid()).remainingDays()).isEqualTo(2.5);
    }
    @Test void acceptsActualPythonEncoderPayload() throws Exception {
        String fixture=new String(getClass().getResourceAsStream("/edge_harvest_fixture.json").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        HarvestPayload p=json.readValue(fixture,HarvestPayload.class);
        assertThat(service.parseAndValidate(fixture,p.observationId(),p.capturedAt(),plant).features()).hasSize(275);
    }
    @Test void featureOnlyPayloadGetsItsDateFromEc2() throws Exception {
        var raw=valid();raw.put("status","FEATURES_READY");raw.put("remainingDays",null);raw.put("expectedAt",null);
        HarvestPayload features=parse(raw);
        var predicted=valid();
        HarvestPayload expected=parse(predicted);
        when(inference.predict(features)).thenReturn(expected);
        assertThat(service.predictOnEc2(features)).isSameAs(expected);
        verify(inference).predict(features);
    }
    @Test void rejectsWrongWidthDateVersionAndMissingFlag() throws Exception {
        var p=valid();p.put("features",List.of(1.,2.));assertThatThrownBy(()->parse(p)).isInstanceOf(ResponseStatusException.class);
        var q=valid();q.put("expectedAt",time);assertThatThrownBy(()->parse(q)).isInstanceOf(ResponseStatusException.class);
        var r=valid();r.put("modelVersion","old");assertThatThrownBy(()->parse(r)).isInstanceOf(ResponseStatusException.class);
        var s=valid();var f=new ArrayList<>(Collections.nCopies(275,0.));f.set(274,1.);s.put("features",f);
        assertThatThrownBy(()->parse(s)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void onlyActiveOriginalPlantCanReceivePhoto() {
        assertThat(service.validateCultivation(device,0,1L,time)).isSameAs(plant);
        device.setPortStatus("00000000");assertThatThrownBy(()->service.validateCultivation(device,0,1L,time)).isInstanceOf(ResponseStatusException.class);
        device.setPortStatus("10000000");plant.finish(PlantEndReason.HARVESTED);
        assertThatThrownBy(()->service.validateCultivation(device,0,1L,time)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void wrongPortAndFutureCaptureAreRejected() {
        assertThatThrownBy(()->service.validateCultivation(device,1,1L,time)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.validateCultivation(device,0,1L,Instant.now().plusSeconds(3600))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void nonLettuceCannotClaimLettuceForecast() {
        plant.getSpecies().setName("딸기");assertThatThrownBy(()->parse(valid())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void latestChecksOwnerAndPortAndDoesNotReturnOldPlant() {
        when(plants.findByIdAndUserId(1L,10L)).thenReturn(Optional.of(plant));
        when(devices.findById("TEST")).thenReturn(Optional.of(device));
        device.setPortStatus("00000000");assertThat(service.latest(1L,10L).status()).isEqualTo("PORT_OFF");
        assertThatThrownBy(()->service.latest(1L,11L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(predictions);
    }
    @Test void authenticationIsPerDeviceAndUnsetKeysDenyAccess() {
        var auth=new DeviceEdgeAuth("{\"TEST\":\"01234567890123456789012345678901\"}");
        auth.verify("TEST","01234567890123456789012345678901");
        assertThat(auth.isConfigured("TEST")).isTrue();
        assertThat(auth.isConfigured("OTHER")).isFalse();
        assertThatThrownBy(()->auth.verify("OTHER","01234567890123456789012345678901")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->new DeviceEdgeAuth("{}").verify("TEST",null)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void springBindsDefaultAndConfiguredDeviceKeys() {
        try(var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(org.springframework.context.support.PropertySourcesPlaceholderConfigurer.class,
                    org.springframework.context.support.PropertySourcesPlaceholderConfigurer::new);
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test",
                    Map.of("growlab.edge.device-keys-json","{\"TEST\":\"01234567890123456789012345678901\"}")));
            context.register(DeviceEdgeAuth.class);context.refresh();
            context.getBean(DeviceEdgeAuth.class).verify("TEST","01234567890123456789012345678901");
        }
    }
}
