package com.metaverse.growlab_be.market_price.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crop_code")
@Getter
@NoArgsConstructor
public class CropCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작물 이름
    @Column(nullable = false)
    private String itemName;

    // KAMIS 품목 코드
    @Column(nullable = false, unique = true)
    private String itemCode;

    // 품종 코드
    @Column(length = 50)
    private String kindCode;

    // 품종 이름
    @Column(length = 100)
    private String kindName;

    // 단위
    @Column(length = 20)
    private String unit;

    @Builder
    public CropCode(String itemName,String itemCode, String kindCode, String kindName, String unit) {
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.kindCode = kindCode;
        this.kindName = kindName;
        this.unit = unit;
    }
}
