package com.metaverse.growlab_be.harvest;

import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.device.dto.PortStateSnapshot;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequiredArgsConstructor
public class HarvestController {
    private final HarvestService service;
    private final DeviceEdgeAuth auth;
    private final DeviceRepository devices;
    private final PlantRepository plants;
    @GetMapping("/api/harvest-predictions/{plantId}")
    public HarvestService.View latest(@PathVariable Long plantId,@AuthenticationPrincipal PrincipalDetails principal) {
        return service.latest(plantId,principal.getUser().getId());
    }
    @GetMapping("/api/device-edge/{serial}/ports") @Transactional(readOnly=true)
    public PortStateSnapshot ports(@PathVariable String serial,@RequestHeader(value="X-Device-Key",required=false) String key) {
        auth.verify(serial,key);
        var d=devices.findById(serial).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return PortStateSnapshot.from(serial,d.getPortStatus(),plants.findByDeviceIdAndEndedAtIsNull(serial));
    }
}
