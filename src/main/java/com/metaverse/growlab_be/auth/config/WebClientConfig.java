package com.metaverse.growlab_be.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kamisWebClient() {
        return WebClient.builder()
                .baseUrl("https://www.kamis.or.kr") // KAMIS API 기본 주소 설정
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
