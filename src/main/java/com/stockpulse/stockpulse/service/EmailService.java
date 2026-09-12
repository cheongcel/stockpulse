package com.stockpulse.stockpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    /** 구독 확인(더블 옵트인) 메일. 이 링크를 눌러야 실제로 다이제스트가 시작된다. */
    public void sendConfirmationEmail(String toEmail, String keyword, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[StockPulse] '" + keyword + "' 구독을 확인해주세요");
            message.setText(
                    "안녕하세요! StockPulse입니다.\n\n" +
                            "'" + keyword + "' 키워드 뉴스 다이제스트를 구독하려면 아래 링크를 눌러 확인해주세요.\n\n" +
                            baseUrl + "/confirm/" + token + "\n\n" +
                            "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.\n\n" +
                            "StockPulse 팀 드림"
            );
            mailSender.send(message);
            log.info("구독 확인 메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("구독 확인 메일 발송 실패: {}", e.getMessage());
        }
    }

    /**
     * 기사별 한 줄 요약이 담긴 다이제스트 메일. articles는 title/link/summary(null 가능)를 담는다.
     * 해지 링크는 구독별 고유 토큰을 사용해, 다른 사람의 구독을 지울 수 없게 한다.
     */
    public void sendNewsDigest(String toEmail, String keyword,
                                List<Map<String, String>> articles, String unsubscribeToken) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("안녕하세요! StockPulse 뉴스 다이제스트입니다.\n\n")
                    .append("키워드: ").append(keyword).append("\n\n");

            int i = 1;
            for (Map<String, String> a : articles) {
                String summary = a.get("summary");
                body.append(i++).append(". ").append(a.get("title")).append("\n");
                if (summary != null && !summary.isBlank()) {
                    body.append("   → ").append(summary).append("\n");
                }
                body.append("   ").append(a.get("link")).append("\n\n");
            }

            body.append("──────────────────\n")
                    .append("전체 분석 보기: ").append(baseUrl).append("/analyze/").append(keyword).append("\n")
                    .append("이 키워드 구독 해지: ").append(baseUrl).append("/unsubscribe/").append(unsubscribeToken)
                    .append("\n\nStockPulse 팀 드림");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[StockPulse] " + keyword + " 오늘의 뉴스 요약");
            message.setText(body.toString());
            mailSender.send(message);
            log.info("다이제스트 메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("다이제스트 메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException(e); // 스케줄러가 발송 성공 여부를 기록할 수 있도록 전파
        }
    }
}