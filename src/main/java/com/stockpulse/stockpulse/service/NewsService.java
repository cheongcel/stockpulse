package com.stockpulse.stockpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    @Value("${news.api.key}")
    private String newsApiKey;

    private final WebClient webClient = WebClient.create("https://newsapi.org");

    public List<String> fetchNewsTitles(String ticker) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/everything")
                            .queryParam("q", ticker)
                            .queryParam("language", "en")
                            .queryParam("sortBy", "publishedAt")
                            .queryParam("pageSize", "5")
                            .queryParam("apiKey", newsApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<String> titles = new ArrayList<>();
            if (response != null && response.has("articles")) {
                for (JsonNode article : response.get("articles")) {
                    titles.add(article.get("title").asText());
                }
            }
            return titles;
        } catch (Exception e) {
            log.error("뉴스 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
