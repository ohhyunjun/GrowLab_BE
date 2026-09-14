package com.metaverse.growlab_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ 추가

@EnableJpaAuditing
@EnableScheduling // ✅ 추가
@SpringBootApplication
public class GrowLabBeApplication {
	public static void main(String[] args) {
		SpringApplication.run(GrowLabBeApplication.class, args);
	}
}
