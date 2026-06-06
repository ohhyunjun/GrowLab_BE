package com.metaverse.growlab_be.sensor_log.domain;

import com.metaverse.growlab_be.device.domain.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sensor_anomaly")
public class SensorAnomaly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serial_number", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    @Column(nullable = false)
    private Float value;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duraiton_min")
    private Integer duraitonMin;

    public SensorAnomaly(Device device, SensorType sensorType, Float value) {
        this.device = device;
        this.sensorType = sensorType;
        this.startedAt = LocalDateTime.now();
        this.value = value;
    }

    public void close(){
        this.endedAt = LocalDateTime.now();
        this.duraitonMin = (int) java.time.Duration.between(
                this.startedAt, this.endedAt).toMinutes();
    }


}
