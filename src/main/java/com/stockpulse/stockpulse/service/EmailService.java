package com.stockpulse.stockpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendNewsDigest(String toEmail, String ticker, String summary, String sentiment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[StockPulse] " + ticker + " 오늘의 뉴스 요약");
            message.setText(
                    "안녕하세요! StockPulse 뉴스 다이제스트입니다.\n\n" +
                            "📈 종목: " + ticker + "\n" +
                            "📊 감성: " + sentiment + "\n\n" +
                            "📰 AI 요약:\n" + summary + "\n\n" +
                            "StockPulse 팀 드림"
            );
            mailSender.send(message);
            log.info("이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
        }
    }
}