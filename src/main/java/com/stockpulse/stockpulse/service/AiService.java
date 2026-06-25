package com.stockpulse.stockpulse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient = WebClient.create("https://generativelanguage.googleapis.com");

    public String analyzeNews(String ticker, List<String> titles) {
        String prompt = String.format(
                "다음은 %s 종목 관련 최신 뉴스 제목들입니다:\n%s\n\n" +
                        "위 뉴스들을 분석해서 아래 JSON 형식으로만 답해주세요. " +
                        "다른 말은 하지 말고 JSON만 출력하세요:\n" +
                        "{\"summary\": \"3줄 요약\", \"sentiment\": \"POSITIVE or NEGATIVE or NEUTRAL\", \"score\": 0.0~1.0}",
                ticker, String.join("\n", titles)
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (attempt > 0) Thread.sleep(attempt * 2000L);

                JsonNode response = webClient.post()
                        .uri("/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                if (response != null) {
                    String text = response
                            .get("candidates").get(0)
                            .get("content").get("parts").get(0)
                            .get("text").asText();
                    text = text.replaceAll("```json", "").replaceAll("```", "").trim();
                    return text;
                }

            } catch (Exception e) {
                log.warn("Gemini 시도 {}/3 실패: {}", attempt + 1, e.getMessage());
            }
        }

        return "{\"summary\": \"잠시 후 다시 시도해주세요\", \"sentiment\": \"NEUTRAL\", \"score\": 0.5}";
    }
}