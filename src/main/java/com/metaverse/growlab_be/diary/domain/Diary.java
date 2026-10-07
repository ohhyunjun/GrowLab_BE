package com.metaverse.growlab_be.diary.domain;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.common.domain.TimeStamped;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.image.domain.Image;
import com.metaverse.growlab_be.plant.domain.Plant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "diary")
public class Diary extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime targetDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_serial", nullable = false)
    private Device device;

    @Column(name = "port_index", nullable = false)
    private Integer portIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", foreignKey = @ForeignKey(name = "fk_diary_plant"))
    private Plant plant;

    // Keep the names from the day of writing even after this port is replanted.
    @Column(name = "species_name")
    private String speciesName;

    @Column(name = "plant_name")
    private String plantName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    public Diary(DiaryRequestDto request, Device device, User user, Plant plant) {
        this.title = request.getTitle();
        this.content = request.getContent();
        this.targetDate = request.getTargetDate();
        this.portIndex = plant.getPortIndex();
        this.device = device;
        this.user = user;
        this.plant = plant;
        this.speciesName = plant.getSpecies().getName();
        this.plantName = plant.getName();
    }

    public void addImage(Image image) {
        image.setDiary(this);
        images.add(image);
    }

    public void update(DiaryRequestDto request) {
        this.title = request.getTitle();
        this.content = request.getContent();
        this.targetDate = request.getTargetDate();
    }
}
