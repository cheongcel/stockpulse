package com.stockpulse.stockpulse.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "news_analysis")
public class NewsAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(nullable = false)
    private String title; // 뉴스 제목

    @Column(columnDefinition = "TEXT")
    private String summary; // AI 3줄 요약

    @Column(nullable = false)
    private String sentiment; // POSITIVE / NEGATIVE / NEUTRAL

    private Double sentimentScore; // 0.0 ~ 1.0

    private LocalDateTime publishedAt;
    private LocalDateTime analyzedAt;

    public NewsAnalysis(Stock stock, String title, String summary,
                        String sentiment, Double sentimentScore,
                        LocalDateTime publishedAt) {
        this.stock = stock;
        this.title = title;
        this.summary = summary;
        this.sentiment = sentiment;
        this.sentimentScore = sentimentScore;
        this.publishedAt = publishedAt;
        this.analyzedAt = LocalDateTime.now();
    }
}