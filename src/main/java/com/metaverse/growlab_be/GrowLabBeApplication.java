package com.metaverse.growlab_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GrowLabBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrowLabBeApplication.class, args);
	}

}
