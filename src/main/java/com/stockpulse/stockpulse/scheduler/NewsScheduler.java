package com.stockpulse.stockpulse.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.stockpulse.domain.Stock;
import com.stockpulse.stockpulse.repository.StockRepository;
import com.stockpulse.stockpulse.service.AiService;
import com.stockpulse.stockpulse.service.EmailService;
import com.stockpulse.stockpulse.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final StockRepository stockRepository;
    private final NewsService newsService;
    private final AiService aiService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyNewsDigest() {
        log.info("뉴스 다이제스트 스케줄러 시작");
        List<Stock> stocks = stockRepository.findAll();

        for (Stock stock : stocks) {
            try {
                List<Map<String, String>> newsList = newsService.fetchNews(stock.getKeyword());
                if (newsList.isEmpty()) continue;

                List<String> titles = newsService.extractTitles(newsList);
                String aiResult = aiService.analyzeNews(stock.getKeyword(), titles);

                String summary;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(aiResult);
                    summary = node.has("summary") ?
                            node.get("summary").asText() : "요약을 가져올 수 없어요.";
                } catch (Exception e) {
                    log.warn("AI 결과 파싱 실패: {}", e.getMessage());
                    summary = "요약을 가져올 수 없어요.";
                }

                emailService.sendNewsDigest(
                        stock.getEmail(),
                        stock.getKeyword(),
                        summary,
                        "분석중"
                );

                // 키워드 사이 10초 대기 (Gemini 429 방지)
                Thread.sleep(10000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("스케줄러 인터럽트: {}", e.getMessage());
            } catch (Exception e) {
                log.error("키워드 처리 실패 {}: {}", stock.getKeyword(), e.getMessage());
            }
        }
        log.info("뉴스 다이제스트 완료");
    }
}