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
                "다음은 '%s' 관련 최신 뉴스 제목 %d개입니다:\n%s\n\n" +
                        "위 뉴스들을 바탕으로 현재 여론과 시장 분위기를 분석해주세요.\n" +
                        "반드시 아래 JSON 형식으로만 답하세요:\n" +
                        "{\n" +
                        "  \"summary\": \"뉴스 전체 흐름을 3문장으로 요약. 구체적 수치나 핵심 사건 포함\",\n" +
                        "  \"sentiment\": \"POSITIVE 또는 NEGATIVE 또는 NEUTRAL\",\n" +
                        "  \"score\": 0.0에서 1.0 사이 숫자\n" +
                        "}",
                ticker, titles.size(), String.join("\n", titles)
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