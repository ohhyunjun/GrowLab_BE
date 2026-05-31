package com.metaverse.growlab_be.market_price.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "rank_code",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rank_code",
                columnNames = "code"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 등급코드 (04, 05 ...)
    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    // 등급명 (상품, 중품 ...)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Builder
    public RankCode(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
