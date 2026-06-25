package com.stockpulse.stockpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.create("https://openapi.naver.com");

    public List<Map<String, String>> fetchNews(String keyword) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/news.json")
                            .queryParam("query", keyword)
                            .queryParam("display", 10)
                            .queryParam("sort", "sim")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<Map<String, String>> newsList = new ArrayList<>();
            if (response != null && response.has("items")) {
                for (JsonNode item : response.get("items")) {
                    String title = item.get("title").asText()
                            .replaceAll("<[^>]*>", "")
                            .replaceAll("&amp;", "&")
                            .replaceAll("&quot;", "\"")
                            .replaceAll("&#039;", "'");
                    String link = item.get("link").asText();

                    Map<String, String> news = new HashMap<>();
                    news.put("title", title);
                    news.put("link", link);
                    newsList.add(news);
                }
            }
            return newsList;

        } catch (Exception e) {
            log.error("네이버 뉴스 수집 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // AI 분석용 제목만 추출
    public List<String> extractTitles(List<Map<String, String>> newsList) {
        return newsList.stream()
                .map(news -> news.get("title"))
                .toList();
    }
}