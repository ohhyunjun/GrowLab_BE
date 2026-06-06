package com.metaverse.growlab_be.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kamisWebClient() {
        return WebClient.builder()
                .baseUrl("https://www.kamis.or.kr")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .jackson2JsonDecoder(
                                new Jackson2JsonDecoder(
                                        new ObjectMapper(),
                                        MediaType.APPLICATION_JSON,
                                        MediaType.TEXT_PLAIN
                                )
                        ))
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
