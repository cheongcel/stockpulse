package com.stockpulse.stockpulse.scheduler;

import com.stockpulse.stockpulse.domain.Stock;
import com.stockpulse.stockpulse.repository.StockRepository;
import com.stockpulse.stockpulse.service.DigestService;
import com.stockpulse.stockpulse.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final StockRepository stockRepository;
    private final DigestService digestService;
    private final EmailService emailService;

    // 매일 오전 7시(한국 시간)에 발송. 배포 서버 시간대와 무관하게 항상 KST 기준으로 동작한다.
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void sendDailyNewsDigest() {
        log.info("뉴스 다이제스트 스케줄러 시작");
        // 이메일 확인(더블 옵트인)을 마친 구독만 발송 대상이다.
        List<Stock> stocks = stockRepository.findAllByConfirmedTrue();

        for (Stock stock : stocks) {
            try {
                DigestService.Digest digest = digestService.getDigest(stock.getKeyword());
                if (digest.isEmpty()) {
                    log.info("새 기사 없음, 발송 생략: {}", stock.getKeyword());
                    continue;
                }

                String latestLink = digest.latestLink();
                if (latestLink != null && latestLink.equals(stock.getLastSentArticleLink())) {
                    log.info("이전과 동일한 최신 기사, 중복 발송 방지로 생략: {}", stock.getKeyword());
                    continue;
                }

                try {
                    emailService.sendNewsDigest(
                            stock.getEmail(), stock.getKeyword(), digest.articles, stock.getToken());
                    stock.setLastSendSuccess(true);
                    stock.setLastSentArticleLink(latestLink);
                } catch (Exception sendFailure) {
                    stock.setLastSendSuccess(false);
                    log.error("이메일 발송 실패, 다음 회차에 재시도됨 {}: {}",
                            stock.getKeyword(), sendFailure.getMessage());
                } finally {
                    stock.setLastSentAt(LocalDateTime.now());
                    stockRepository.save(stock);
                }

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