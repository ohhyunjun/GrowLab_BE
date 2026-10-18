package com.metaverse.growlab_be.device.dto;

import com.metaverse.growlab_be.plant.domain.Plant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Full cultivation snapshot. A port number is not a cultivation identity. */
public record PortStateSnapshot(int schemaVersion, String serialNumber, String generatedAt,
                                String portStatus, List<Port> ports) {
    public record Port(int portIndex, boolean active, Long plantId, Long speciesId, String plantedAt, String predictionCrop) {}

    public static PortStateSnapshot from(String serial, String portStatus, List<Plant> plants) {
        Map<Integer, Plant> byPort = new HashMap<>();
        for (Plant plant : plants) {
            Integer index = plant.getPortIndex();
            if (!plant.isActive() || index == null || index < 0 || index >= 8) continue;
            if (plant.getDevice() == null || !serial.equals(plant.getDevice().getId())) {
                throw new IllegalArgumentException("다른 기기의 식물은 포트 상태에 포함할 수 없습니다.");
            }
            if (byPort.put(index, plant) != null) {
                throw new IllegalStateException("동일 포트에 재배 중인 식물이 중복됩니다.");
            }
        }
        List<Port> ports = new ArrayList<>();
        StringBuilder effectiveStatus = new StringBuilder();
        for (int index = 0; index < 8; index++) {
            Plant plant = byPort.get(index);
            boolean active = plant != null && plant.getId() != null && portStatus != null
                    && portStatus.length() > index && portStatus.charAt(index) == '1';
            effectiveStatus.append(active ? '1' : '0');
            ports.add(new Port(index, active, plant == null ? null : plant.getId(),
                    plant == null || plant.getSpecies() == null ? null : plant.getSpecies().getId(),
                    plant == null || plant.getPlantedAt() == null ? null : plant.getPlantedAt().toString(),
                    plant != null && com.metaverse.growlab_be.harvest.HarvestCrop.supports(plant.getSpecies()) ? "lettuce" : null));
        }
        return new PortStateSnapshot(1, serial, Instant.now().toString(), effectiveStatus.toString(), List.copyOf(ports));
    }
}
