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
    private Device device;

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

    @Column(name = "led_status")
    private Boolean led_status;

    @Column(name = "led_on_minutes_1h")
    private Integer led_on_minutes_1h;

    public SensorLog(
            Device device,
            Float temperature,
            Float humidity,
            Float ph,
            Float tds,
            Boolean water_level_status,
            Boolean led_status,
            Integer led_on_minutes_1h
    ) {
        this.device = device;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ph = ph;
        this.tds = tds;
        this.water_level_status = water_level_status;
        this.led_status = led_status;
        this.led_on_minutes_1h = led_on_minutes_1h;
    }
}
