package com.metaverse.growlab_be.device.domain;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.common.domain.TimeStamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Device(String id, String deviceNickname) {
        this.id = id;
        this.deviceNickname = deviceNickname;
        this.status = false;
    }
}
