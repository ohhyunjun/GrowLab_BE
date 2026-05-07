package com.metaverse.growlab_be.sensor_log.domain;

import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sensor_log")
public class SensorLog extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serial_number", nullable = false)
    private Device serialNumber;

    @Column()
    private Float temperature;

    @Column()
    private Float humidity;

    @Column()
    private Float ph;

    @Column()
    private Float tds;

    @Column()
    private Boolean water_level_status;

    public SensorLog(Device serial_number, Float temperature, Float humidity, Float ph, Float tds, Boolean water_level_status) {
        this.serialNumber = serial_number;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ph = ph;
        this.tds = tds;
        this.water_level_status = water_level_status;
    }
}
