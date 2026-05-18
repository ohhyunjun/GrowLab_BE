package com.metaverse.growlab_be.device.domain;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.plant.domain.Plant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device")
public class Device extends TimeStamped {

    @Id
    @Column(name = "serial_number")
    private String id;

    @Column(name="device_nickname")
    private String deviceNickname;

    @Column(name="status")
    private Boolean status;

    @Column(name="led_status")
    private Boolean ledStatus = false;

    @Column(name = "led_mode")
    private Boolean ledMode = false; //true = auto, false = notauto

    @Column(name = "led_on_time")
    private LocalTime ledOnTime;

    @Column(name = "led_off_time")
    private LocalTime ledOffTime;

    @Column(name = "photo_interval")
    private Integer photoInterval = 12;

    @Column(name = "port_status")
    private String portStatus = "00000000";

    // User와의 N:1 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Plant와의 1:N 관계 설정
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Plant> plants = new ArrayList<>();

    public Device(String id, String deviceNickname) {
        this.id = id;
        this.deviceNickname = deviceNickname;
        this.status = false;
        this.ledStatus = false;
        this.photoInterval = 12;
    }
}
