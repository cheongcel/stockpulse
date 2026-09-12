package com.stockpulse.stockpulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 기사별 한 줄 요약을 생성한다. 감성/점수는 더 이상 제공하지 않는다.
 * AI 호출이 끝내 실패하면 null 리스트를 반환하고, 실패 문구를 요약처럼 꾸며내지 않는다.
 */
@Slf4j
@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient = WebClient.create("https://generativelanguage.googleapis.com");
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param articles title/description 이 담긴 뉴스 목록 (NewsService.fetchNews 결과)
     * @return 입력과 같은 순서·같은 개수의 한 줄 요약. 실패한 항목은 null.
     */
    public List<String> summarizeEach(String keyword, List<Map<String, String>> articles) {
        List<String> result = new ArrayList<>();
        for (Map<String, String> a : articles) result.add(null);
        if (articles.isEmpty()) return result;

        StringBuilder itemsText = new StringBuilder();
        for (int i = 0; i < articles.size(); i++) {
            Map<String, String> a = articles.get(i);
            itemsText.append(i).append(". 제목: ").append(a.getOrDefault("title", ""))
                    .append(" / 본문 일부: ").append(a.getOrDefault("description", ""))
                    .append("\n");
        }

        String prompt = String.format(
                "다음은 '%s' 관련 최신 뉴스 %d건입니다. 각 기사에 인덱스가 붙어 있습니다:\n%s\n\n" +
                        "각 기사를 한 문장(40자 내외)으로 요약하세요. " +
                        "반드시 주어진 제목과 본문 일부 안의 내용만 사용하고, 없는 사실을 추측하거나 지어내지 마세요.\n" +
                        "반드시 아래 JSON 형식으로만, 입력과 같은 개수·같은 순서로 답하세요:\n" +
                        "{ \"items\": [\"0번 기사 한 줄 요약\", \"1번 기사 한 줄 요약\", ...] }",
                keyword, articles.size(), itemsText
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

                    JsonNode node = mapper.readTree(text);
                    JsonNode items = node.get("items");
                    if (items != null && items.isArray()) {
                        for (int i = 0; i < result.size() && i < items.size(); i++) {
                            result.set(i, items.get(i).asText(null));
                        }
                    }
                    return result;
                }

            } catch (Exception e) {
                log.warn("Gemini 시도 {}/3 실패: {}", attempt + 1, e.getMessage());
            }
        }

        log.error("Gemini 요약 최종 실패 (키워드: {}) - 기사 제목/링크만 노출됩니다", keyword);
        return result; // 전부 null: 실패를 요약처럼 꾸며내지 않음
    }
}