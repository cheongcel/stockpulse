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

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.create("https://openapi.naver.com");

    public List<String> fetchNewsTitles(String keyword) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/news.json")
                            .queryParam("query", keyword)
                            .queryParam("display", 5)
                            .queryParam("sort", "date")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<String> titles = new ArrayList<>();
            if (response != null && response.has("items")) {
                for (JsonNode item : response.get("items")) {
                    // HTML 태그 제거
                    String title = item.get("title").asText()
                            .replaceAll("<[^>]*>", "")
                            .replaceAll("&amp;", "&")
                            .replaceAll("&quot;", "\"")
                            .replaceAll("&#039;", "'");
                    titles.add(title);
                }
            }
            return titles;

        } catch (Exception e) {
            log.error("네이버 뉴스 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }
}