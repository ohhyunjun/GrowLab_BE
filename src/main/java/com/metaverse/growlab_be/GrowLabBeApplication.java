package com.metaverse.growlab_be;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ 추가

import java.util.TimeZone;

@EnableJpaAuditing
@EnableScheduling // ✅ 추가
@SpringBootApplication
public class GrowLabBeApplication {

	// 컨테이너/서버 OS 시간대가 UTC여도 앱은 항상 한국시각(KST)으로 동작하게 고정한다.
	// @CreatedDate(LocalDateTime.now())·스케줄러·발아/수확 기록이 모두 KST 벽시계로 저장되어
	// 클라이언트가 보내는 plantedAt(KST)과 어긋나지 않는다. (DB는 타임존 없는 datetime에 그대로 저장)
	@PostConstruct
	public void setDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(GrowLabBeApplication.class, args);
	}
}
