package com.stockpulse.stockpulse.scheduler;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final StockRepository stockRepository;
    private final NewsService newsService;
    private final AiService aiService;
    private final EmailService emailService;

    // 매일 아침 7시 자동 실행
    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyNewsDigest() {
        log.info("뉴스 다이제스트 스케줄러 시작");
        List<Stock> stocks = stockRepository.findAll();

        for (Stock stock : stocks) {
            List<String> titles = newsService.fetchNewsTitles(stock.getTicker());
            if (titles.isEmpty()) continue;

            String aiResult = aiService.analyzeNews(stock.getTicker(), titles);
            emailService.sendNewsDigest(
                    stock.getEmail(),
                    stock.getTicker(),
                    aiResult,
                    "분석중"
            );
        }
        log.info("뉴스 다이제스트 완료");
    }
}